package com.example.demo.service.impl;

import com.example.demo.dto.CsvAnalysisResult;
import com.example.demo.dto.CsvColumnAnalysis;
import com.example.demo.service.CsvEtlService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CsvEtlServiceImpl implements CsvEtlService {

    @Override
    public CsvAnalysisResult analyzeCsv(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                throw new RuntimeException("El archivo CSV está vacío.");
            }

            char delimiter = detectDelimiter(content);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();

            int total = 0;
            Map<String, Integer> nullCounts = new HashMap<>();
            Map<String, Map<String, Integer>> valueCounts = new HashMap<>();

            try (CSVParser parser = new CSVParser(new StringReader(content), format)) {
                List<String> headers = parser.getHeaderNames();

                for (CSVRecord row : parser) {
                    total++;
                    for (String col : headers) {
                        String raw = row.isMapped(col) ? row.get(col) : null;
                        String val = (raw == null ? "" : raw.trim());

                        if (val.isEmpty()) {
                            nullCounts.put(col, nullCounts.getOrDefault(col, 0) + 1);
                        } else {
                            Map<String, Integer> vc =
                                    valueCounts.computeIfAbsent(col, k -> new HashMap<>());
                            vc.put(val, vc.getOrDefault(val, 0) + 1);
                        }
                    }
                }

                List<CsvColumnAnalysis> cols = new ArrayList<>();
                for (String col : valueCounts.keySet()) {
                    int nullCount = nullCounts.getOrDefault(col, 0);
                    double nullPct = total > 0 ? (nullCount * 1.0 / total) : 0.0;

                    Map<String, Integer> vc = valueCounts.get(col);
                    int dupCount = 0;
                    for (int c : vc.values()) {
                        if (c > 1) {
                            dupCount += (c - 1);
                        }
                    }
                    double dupPct = total > 0 ? (dupCount * 1.0 / total) : 0.0;

                    cols.add(CsvColumnAnalysis.builder()
                            .name(col)
                            .nullCount(nullCount)
                            .nullPercent(nullPct)
                            .duplicateCount(dupCount)
                            .duplicatePercent(dupPct)
                            .build());
                }

                return CsvAnalysisResult.builder()
                        .totalRows(total)
                        .columns(cols)
                        .build();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error leyendo el CSV", e);
        }
    }

    @Override
    public byte[] etlAndExport(MultipartFile file,
                               Set<String> selectedColumns,
                               String keyColumn) {

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                throw new RuntimeException("El archivo CSV está vacío.");
            }

            char delimiter = detectDelimiter(content);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();

            boolean allColumnsSelected = (selectedColumns == null || selectedColumns.isEmpty());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (CSVParser parser = new CSVParser(new StringReader(content), format)) {

                List<String> headers = parser.getHeaderNames();

                // 1) Determinar columnas a exportar
                List<String> exportCols = new ArrayList<>();
                if (allColumnsSelected) {
                    exportCols.addAll(headers);
                } else {
                    for (String h : headers) {
                        if (selectedColumns.stream().anyMatch(c -> c.equalsIgnoreCase(h))) {
                            exportCols.add(h);
                        }
                    }
                }

                if (exportCols.isEmpty()) {
                    throw new RuntimeException("Ninguna de las columnas seleccionadas existe en el CSV.");
                }

                // 2) Ver si realmente podemos deduplicar (keyColumn existe)
                boolean useKey = (keyColumn != null && !keyColumn.isBlank()
                        && headers.stream().anyMatch(h -> h.equalsIgnoreCase(keyColumn)));

                String realKeyColumn = null;
                if (useKey) {
                    for (String h : headers) {
                        if (h.equalsIgnoreCase(keyColumn)) {
                            realKeyColumn = h;
                            break;
                        }
                    }
                }

                Map<String, List<String>> rowsByKey = useKey ? new LinkedHashMap<>() : null;
                List<List<String>> rowsList = useKey ? null : new ArrayList<>();

                int rowIndex = 0;

                for (CSVRecord row : parser) {
                    rowIndex++;

                    List<String> cleanedRow = new ArrayList<>();
                    boolean allEmpty = true;

                    for (String col : exportCols) {
                        String raw = safeGet(row, col);
                        String norm = normalize(raw);

                        if (norm != null && !norm.isBlank()) {
                            allEmpty = false;
                        }

                        cleanedRow.add(csv(norm));
                    }

                    // Si todas las columnas seleccionadas están vacías -> descartamos fila
                    if (allEmpty) {
                        continue;
                    }

                    if (useKey && realKeyColumn != null) {
                        // deduplicar
                        String rawKey = safeGet(row, realKeyColumn);
                        String normKey = normalize(rawKey);
                        String key = (normKey == null || normKey.isBlank())
                                ? "#ROW_" + rowIndex
                                : normKey;
                        rowsByKey.put(key, cleanedRow); // última aparición gana
                    } else {
                        // sin clave -> no deduplicamos
                        rowsList.add(cleanedRow);
                    }
                }

                // 3) Escribir CSV final
                try (PrintWriter writer =
                             new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

                    writer.write('\uFEFF'); // BOM UTF-8
                    writer.println(String.join(String.valueOf(delimiter), exportCols));

                    if (useKey) {
                        for (List<String> rowOut : rowsByKey.values()) {
                            writer.println(String.join(String.valueOf(delimiter), rowOut));
                        }
                    } else {
                        for (List<String> rowOut : rowsList) {
                            writer.println(String.join(String.valueOf(delimiter), rowOut));
                        }
                    }

                    writer.flush();
                }

                return baos.toByteArray();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error aplicando ETL y exportando CSV", e);
        }
    }

    // ===== Helpers =====

    /** Detecta si el separador es ';', ',' o '\t' mirando la primera línea */
    private char detectDelimiter(String content) {
        String firstLine;
        int idx = content.indexOf('\n');
        if (idx >= 0) {
            firstLine = content.substring(0, idx);
        } else {
            firstLine = content;
        }

        int countComma = countChar(firstLine, ',');
        int countSemicolon = countChar(firstLine, ';');
        int countTab = countChar(firstLine, '\t');

        // Elegimos el que más aparezca
        if (countSemicolon >= countComma && countSemicolon >= countTab && countSemicolon > 0) {
            return ';';
        } else if (countTab >= countComma && countTab > 0) {
            return '\t';
        } else {
            return ','; // por defecto
        }
    }

    private int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) n++;
        }
        return n;
    }

    private String safeGet(CSVRecord row, String column) {
        if (column == null) return "";
        if (!row.isMapped(column)) return "";
        String val = row.get(column);
        return val == null ? "" : val;
    }

    private String normalize(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        boolean hasSpecial = text.contains("\"")
                || text.contains("\n")
                || text.contains("\r");
        // OJO: como usamos ; o \t, no tratamos la coma como especial
        if (hasSpecial) {
            text = text.replace("\"", "\"\"");
            return "\"" + text + "\"";
        }
        return text;
    }
}
