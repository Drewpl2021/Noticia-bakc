package com.example.demo.service.impl;

import com.example.demo.dto.DatamartAnalysisResult;
import com.example.demo.dto.DimensionCandidateDto;
import com.example.demo.dto.MeasureCandidateDto;
import com.example.demo.service.DatamartService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatamartServiceImpl implements DatamartService {

    private final JdbcTemplate jdbcTemplate;

    // =================== ANALISIS ===================

    @Override
    public DatamartAnalysisResult analyzeForDatamart(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                throw new RuntimeException("El archivo CSV está vacío");
            }

            char delimiter = detectDelimiter(content);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();

            List<DimensionCandidateDto> dimensionCandidates = new ArrayList<>();
            List<MeasureCandidateDto> measureCandidates = new ArrayList<>();

            int totalRows = 0;
            Map<String, Set<String>> distinctValues = new HashMap<>();
            Map<String, Boolean> isNumeric = new HashMap<>();

            try (CSVParser parser = new CSVParser(new StringReader(content), format)) {
                List<String> headers = parser.getHeaderNames();

                // inicializar estructuras
                for (String h : headers) {
                    distinctValues.put(h, new HashSet<>());
                    isNumeric.put(h, true); // asumimos que es numérico hasta que se demuestre lo contrario
                }

                for (CSVRecord row : parser) {
                    totalRows++;

                    for (String col : headers) {
                        String raw = safeGet(row, col);
                        String val = raw == null ? "" : raw.trim();

                        if (!val.isEmpty()) {
                            distinctValues.get(col).add(val);

                            // validar si sigue siendo numérico
                            if (isNumeric.get(col)) {
                                if (!isNumber(val)) {
                                    isNumeric.put(col, false);
                                }
                            }
                        }
                    }
                }

                // construir candidatos
                for (String col : distinctValues.keySet()) {
                    Set<String> vals = distinctValues.get(col);
                    long distinctCount = vals.size();
                    double ratio = (totalRows > 0) ? (distinctCount * 1.0 / totalRows) : 0.0;

                    boolean numeric = isNumeric.getOrDefault(col, false);

                    // Heurística simple:
                    // - dimensión: columna NO numérica o numérica con pocos valores únicos comparado al total
                    // - medida: columna numérica
                    boolean suggestedDimension =
                            (!numeric && distinctCount > 1 && distinctCount <= Math.max(50, totalRows * 0.8))
                                    || (numeric && distinctCount > 1 && distinctCount <= Math.max(20, totalRows * 0.5));

                    dimensionCandidates.add(
                            DimensionCandidateDto.builder()
                                    .name(col)
                                    .distinctCount(distinctCount)
                                    .distinctRatio(ratio)
                                    .suggested(suggestedDimension)
                                    .sampleValues(vals.stream().limit(5).collect(Collectors.toList()))
                                    .build()
                    );

                    if (numeric) {
                        measureCandidates.add(
                                MeasureCandidateDto.builder()
                                        .name(col)
                                        .numeric(true)
                                        .build()
                        );
                    }
                }

                // sugerimos máximo 5 dimensiones
                int maxDims = Math.min(5, dimensionCandidates.size());

                return DatamartAnalysisResult.builder()
                        .totalRows(totalRows)
                        .maxDimensionsSuggested(maxDims)
                        .dimensionCandidates(dimensionCandidates)
                        .measureCandidates(measureCandidates)
                        .build();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error analizando CSV para datamart", e);
        }
    }

    // =================== CONSTRUCCION ===================

    @Override
    public void buildDatamart(MultipartFile file,
                              String datamartName,
                              List<String> dimensionCols,
                              List<String> measureCols) {

        if (dimensionCols == null || dimensionCols.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una dimensión");
        }

        // sanitizar nombre de datamart y columnas para evitar SQL injection simple
        String dmName = sanitizeName(datamartName);
        List<String> dims = dimensionCols.stream()
                .map(this::sanitizeName)
                .toList();
        List<String> measures = (measureCols == null) ? List.of() :
                measureCols.stream().map(this::sanitizeName).toList();

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                throw new RuntimeException("El archivo CSV está vacío");
            }

            char delimiter = detectDelimiter(content);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();

            // 1) Crear tablas de dimensiones y tabla de hechos
            createDatamartTables(dmName, dims, measures);

            // Map para cachear ids de dimensiones: dimName -> (value -> id)
            Map<String, Map<String, Long>> dimCaches = new HashMap<>();
            for (String dim : dims) {
                dimCaches.put(dim, new HashMap<>());
            }

            String factTable = "dm_" + dmName + "_fact";

            try (CSVParser parser = new CSVParser(new StringReader(content), format)) {
                List<String> headers = parser.getHeaderNames();

                for (CSVRecord row : parser) {

                    // 2) Para cada dimensión, insertar/obtener id en su tabla
                    Map<String, Long> dimIds = new HashMap<>();

                    for (String dimCol : dims) {
                        // nombre de columna en el CSV (puede que no coincida EXACTO con dimCol saneado)
                        String csvCol = findHeaderIgnoreCase(headers, dimCol);
                        if (csvCol == null) {
                            continue; // columna no existe en CSV, se deja null
                        }

                        String rawVal = safeGet(row, csvCol);
                        String val = (rawVal == null || rawVal.trim().isEmpty())
                                ? null
                                : rawVal.trim();

                        Long idDim = null;
                        if (val != null) {
                            Map<String, Long> cache = dimCaches.get(dimCol);
                            if (cache.containsKey(val)) {
                                idDim = cache.get(val);
                            } else {
                                idDim = insertDimensionValue(dmName, dimCol, val);
                                cache.put(val, idDim);
                            }
                        }
                        dimIds.put(dimCol, idDim);
                    }

                    // 3) Construir medidas
                    Map<String, Double> measureValues = new HashMap<>();
                    for (String mCol : measures) {
                        String csvCol = findHeaderIgnoreCase(headers, mCol);
                        if (csvCol == null) continue;
                        String rawVal = safeGet(row, csvCol);
                        Double numeric = parseDoubleOrNull(rawVal);
                        measureValues.put(mCol, numeric);
                    }

                    // 4) Insertar fila de hechos
                    insertFactRow(factTable, dims, dimIds, measures, measureValues);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error construyendo datamart", e);
        }
    }

    // =================== HELPERS ===================

    private void createDatamartTables(String dmName,
                                      List<String> dims,
                                      List<String> measures) {

        // tabla de hechos: dm_<dmName>_fact
        String factTable = "dm_" + dmName + "_fact";

        // crear tablas de dimensiones
        for (String dim : dims) {
            String dimTable = "dm_" + dmName + "_dim_" + dim;
            String sqlDrop = "DROP TABLE IF EXISTS " + dimTable;
            String sqlCreate = "CREATE TABLE " + dimTable + " (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "value VARCHAR(255) NOT NULL" +
                    ")";
            jdbcTemplate.execute(sqlDrop);
            jdbcTemplate.execute(sqlCreate);
        }

        // crear tabla de hechos
        StringBuilder sb = new StringBuilder();
        sb.append("DROP TABLE IF EXISTS ").append(factTable);
        jdbcTemplate.execute(sb.toString());

        sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(factTable).append(" (");
        sb.append("id BIGINT AUTO_INCREMENT PRIMARY KEY,");

        // columnas FK a dimensiones
        for (String dim : dims) {
            sb.append(dim).append("_id BIGINT,");
        }

        // columnas de medidas
        for (String m : measures) {
            sb.append(m).append(" DOUBLE,");
        }

        // quitar coma final
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");

        jdbcTemplate.execute(sb.toString());
    }

    private Long insertDimensionValue(String dmName, String dimCol, String value) {
        String dimTable = "dm_" + dmName + "_dim_" + dimCol;
        jdbcTemplate.update("INSERT INTO " + dimTable + " (value) VALUES (?)", value);
        // obtener last insert id (asumiendo MySQL)
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id;
    }

    private void insertFactRow(String factTable,
                               List<String> dims,
                               Map<String, Long> dimIds,
                               List<String> measures,
                               Map<String, Double> measureValues) {

        StringBuilder cols = new StringBuilder();
        StringBuilder qs = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (String dim : dims) {
            cols.append(dim).append("_id,");
            qs.append("?,");
            params.add(dimIds.get(dim)); // puede ser null
        }

        for (String m : measures) {
            cols.append(m).append(",");
            qs.append("?,");
            params.add(measureValues.get(m)); // puede ser null
        }

        if (cols.length() == 0) {
            return; // nada que insertar
        }

        cols.deleteCharAt(cols.length() - 1);
        qs.deleteCharAt(qs.length() - 1);

        String sql = "INSERT INTO " + factTable +
                " (" + cols + ") VALUES (" + qs + ")";

        jdbcTemplate.update(sql, params.toArray());
    }

    // detecta delimitador ; , o tab
    private char detectDelimiter(String content) {
        String firstLine;
        int idx = content.indexOf('\n');
        firstLine = (idx >= 0) ? content.substring(0, idx) : content;

        int countComma = countChar(firstLine, ',');
        int countSemicolon = countChar(firstLine, ';');
        int countTab = countChar(firstLine, '\t');

        if (countSemicolon >= countComma && countSemicolon >= countTab && countSemicolon > 0) {
            return ';';
        } else if (countTab >= countComma && countTab > 0) {
            return '\t';
        } else {
            return ','; // default
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

    private boolean isNumber(String s) {
        try {
            Double.parseDouble(s.replace(",", "."));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private String sanitizeName(String name) {
        if (name == null) return "dm";
        // solo letras, números y guiones bajos
        String s = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_");
        if (s.isBlank()) return "dm";
        return s;
    }

    private String findHeaderIgnoreCase(List<String> headers, String col) {
        for (String h : headers) {
            if (h.equalsIgnoreCase(col)) {
                return h;
            }
        }
        return null;
    }

    @Override
    public byte[] buildDatamartAndExportCsv(MultipartFile file,
                                            String datamartName,
                                            List<String> dimensionCols,
                                            List<String> measureCols) {
        // 1) Construye tablas y carga datos
        buildDatamart(file, datamartName, dimensionCols, measureCols);

        // 2) Exporta a CSV leyendo el datamart ya construido
        return exportDatamartCsv(datamartName, dimensionCols, measureCols);
    }
    private byte[] exportDatamartCsv(String datamartName,
                                     List<String> dimensionCols,
                                     List<String> measureCols) {

        String dmName = sanitizeName(datamartName);
        List<String> dims = (dimensionCols == null) ? List.of()
                : dimensionCols.stream().map(this::sanitizeName).toList();
        List<String> measures = (measureCols == null) ? List.of()
                : measureCols.stream().map(this::sanitizeName).toList();

        String factTable = "dm_" + dmName + "_fact";

        // 1) SELECT: columnas a devolver en el CSV
        List<String> selectAliases = new ArrayList<>();
        StringBuilder sql = new StringBuilder();

        // SELECT f.id
        sql.append("SELECT f.id AS id");
        selectAliases.add("id");

        // SELECT dimensiones: d1.value AS dim
        int idx = 1;
        for (String dim : dims) {
            String dimAlias = "d" + idx;
            sql.append(", ").append(dimAlias).append(".value AS ").append(dim);
            selectAliases.add(dim);
            idx++;
        }

        // SELECT medidas: f.medida AS medida
        for (String m : measures) {
            sql.append(", f.").append(m).append(" AS ").append(m);
            selectAliases.add(m);
        }

        // 2) FROM y JOINs
        sql.append(" FROM ").append(factTable).append(" f");

        idx = 1;
        for (String dim : dims) {
            String dimAlias = "d" + idx;
            String dimTable = "dm_" + dmName + "_dim_" + dim;

            sql.append(" LEFT JOIN ").append(dimTable).append(" ").append(dimAlias)
                    .append(" ON f.").append(dim).append("_id = ").append(dimAlias).append(".id");

            idx++;
        }

        String finalSql = sql.toString();

        // 3) Ejecutar y exportar a CSV
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(finalSql);

        try (PrintWriter writer =
                     new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            // Cabecera
            writer.write('\uFEFF');
            writer.println(String.join(",", selectAliases));

            // Filas
            for (Map<String, Object> row : rows) {
                List<String> fields = new ArrayList<>();
                for (String col : selectAliases) {
                    Object val = row.get(col);
                    fields.add(csv(val));
                }
                writer.println(String.join(",", fields));
            }
            writer.flush();
        }

        return baos.toByteArray();
    }

    private String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        boolean hasSpecial = text.contains(",") || text.contains("\"")
                || text.contains("\n") || text.contains("\r");
        if (hasSpecial) {
            text = text.replace("\"", "\"\"");
            return "\"" + text + "\"";
        }
        return text;
    }

    @Override
    public byte[] buildDatamartAndExportZip(MultipartFile file,
                                            String datamartName,
                                            List<String> dimensionCols,
                                            List<String> measureCols) {
        // 1) Construir datamart (tablas + carga)
        buildDatamart(file, datamartName, dimensionCols, measureCols);

        // 2) Exportar todas las tablas del datamart a un ZIP
        return exportDatamartZip(datamartName, dimensionCols, measureCols);
    }

    private byte[] exportDatamartZip(String datamartName,
                                     List<String> dimensionCols,
                                     List<String> measureCols) {

        String dmName = sanitizeName(datamartName);
        List<String> dims = (dimensionCols == null) ? List.of()
                : dimensionCols.stream().map(this::sanitizeName).toList();
        List<String> measures = (measureCols == null) ? List.of()
                : measureCols.stream().map(this::sanitizeName).toList();

        String factTable = "dm_" + dmName + "_fact";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            // 1) Exportar tabla de hechos
            byte[] factCsv = exportTableAsCsv(factTable);
            ZipEntry factEntry = new ZipEntry(factTable + ".csv");
            zos.putNextEntry(factEntry);
            zos.write(factCsv);
            zos.closeEntry();

            // 2) Exportar cada tabla dimensión
            for (String dim : dims) {
                String dimTable = "dm_" + dmName + "_dim_" + dim;
                byte[] dimCsv = exportTableAsCsv(dimTable);
                ZipEntry dimEntry = new ZipEntry(dimTable + ".csv");
                zos.putNextEntry(dimEntry);
                zos.write(dimCsv);
                zos.closeEntry();
            }

            zos.finish();
            zos.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error exportando datamart como ZIP", e);
        }
    }

    private byte[] exportTableAsCsv(String tableName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Leemos todas las filas
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName);

        // Si no hay filas, igual sacamos cabecera a partir del DESCRIBE
        List<String> columnNames;
        if (!rows.isEmpty()) {
            // usamos las claves del primer row como cabecera
            columnNames = new ArrayList<>(rows.get(0).keySet());
        } else {
            // MySQL específico: obtener columnas con DESCRIBE
            List<Map<String, Object>> cols = jdbcTemplate.queryForList("DESCRIBE " + tableName);
            columnNames = cols.stream()
                    .map(m -> (String) m.get("Field"))
                    .toList();
        }

        try (PrintWriter writer =
                     new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            writer.write('\uFEFF'); // BOM
            // cabecera
            writer.println(String.join(",", columnNames));

            // filas
            for (Map<String, Object> row : rows) {
                List<String> fields = new ArrayList<>();
                for (String col : columnNames) {
                    Object val = row.get(col);
                    fields.add(csv(val));
                }
                writer.println(String.join(",", fields));
            }

            writer.flush();
        }

        return baos.toByteArray();
    }

}
