package com.example.demo.ejecutable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class ScraperNoticias {

    // ================== Config MySQL (Laragon local) ==================
    private static final String DB_URL =
            "jdbc:mysql://noticias.cng9drffpf1p.us-east-1.rds.amazonaws.com:3306/noticias?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    // ================== Scraper config ==================
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 20_000;
    private static final int SLEEP_MS = 350;     // pausa entre requests
    private static final int MAX_ARTICLES = 120; // límite de noticias por corrida

    // ===== reemplaza estas constantes =====
    private static final List<String> HOME_URLS = List.of(
            "https://diariosinfronteras.com.pe/",
            "https://rpp.pe/",
            "https://diariosinfronteras.com.pe/"

    );

    // dominios válidos (para validar que el enlace pertenece al sitio)
    private static final List<String> DOMAIN_PREFIXES = List.of(
            "https://diariosinfronteras.com.pe/",
            "https://rpp.pe/",
            "https://diariosinfronteras.com.pe/"

    );

    // patrón clásico con fecha /YYYY/MM/DD/
    private static final Pattern PATTERN_POST = Pattern.compile("/20\\d{2}/\\d{2}/\\d{2}/");
    // patrón alterno para sitios que no usan fecha (p. ej. La República)
// patrón alterno: cualquier URL que contenga "noticia"
    private static final Pattern PATTERN_NOTICIA =
            Pattern.compile("noticia-\\d+", Pattern.CASE_INSENSITIVE);

    public static int runOnce(String homeUrl) {
        int count = 0;
        try {
            System.out.println("=== Ejecutando scraper para: " + homeUrl + " ===");

            List<String> enlaces = getHomepageArticleLinks(homeUrl);

            for (String urlNoticia : enlaces) {
                if (count >= MAX_ARTICLES) break;

                try {
                    Articulo art = extractArticle(urlNoticia);

                    // Si no vino categoría desde la página, infiere por el título
                    if (art.categorias == null || art.categorias.isBlank()) {
                        art.categorias = inferCategoria(art.titulo);
                    }

                    System.out.println("Título: " +
                            (art.titulo != null && !art.titulo.isBlank() ? art.titulo : "(sin título)"));
                    System.out.println("Enlace: " + urlNoticia);
                    if (art.imagenUrl != null) System.out.println("Imagen: " + art.imagenUrl);
                    System.out.println();

                    saveArticle(art);
                    count++;
                    Thread.sleep(SLEEP_MS);

                } catch (Exception ex) {
                    System.err.println("Error procesando: " + urlNoticia + " -> " + ex.getMessage());
                }
            }

            System.out.println("✅ Proceso finalizado para " + homeUrl + ". Total guardadas: " + count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }



    // ================== Modelo ==================
    static class Articulo {
        String url;
        String titulo;
        String autor;
        LocalDateTime fechaPublicado; // UTC
        String imagenUrl;
        String contenido;
        String tags;
        String categorias;
    }

    // ================== Util ==================
    private static String cleanText(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }

    private static Element firstNonNull(Element... els) {
        for (Element e : els) if (e != null) return e;
        return null;
    }

    private static LocalDateTime parseIsoDatetimeOrNull(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            String norm = raw.replace("Z", "+00:00");
            OffsetDateTime odt = OffsetDateTime.parse(norm, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String absolutizeUrl(String base, String href) {
        if (href == null || href.isBlank()) return null;
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        if (href.startsWith("//")) return "https:" + href;
        if (href.startsWith("/")) return base.replaceAll("/+$", "") + href;
        return base + (base.endsWith("/") ? "" : "/") + href;
    }

    private static boolean isLikelyPost(String url) {
        if (url == null) return false;

        // debe pertenecer a alguno de los dominios permitidos
        boolean allowedDomain = DOMAIN_PREFIXES.stream().anyMatch(url::startsWith);
        if (!allowedDomain) return false;

        String lower = url.toLowerCase(Locale.ROOT);

        // 1) URLs con fecha tipo /2024/05/10/ (para diarios con fecha en la ruta)
        if (PATTERN_POST.matcher(lower).find()) {
            return true;
        }

        // 2) URLs con 'noticia-<id>' (como RPP)
        if (PATTERN_NOTICIA.matcher(lower).find()) {
            return true;
        }

        return false;
    }


    // ================== Scraping ==================

    /** Lee la portada y devuelve enlaces únicos a posts. */
    private static List<String> getHomepageArticleLinks(String homeUrl) throws Exception {
        Document doc = Jsoup.connect(homeUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .referrer("https://www.google.com/")
                .get();

        Set<String> enlaces = new LinkedHashSet<>();

        // 👇 CASO ESPECIAL: RPP
        if (homeUrl.contains("rpp.pe")) {
            // Todas las anclas que parezcan noticia: ...-noticia-<id>
            Elements anchorsNoticias = doc.select("a[href*='-noticia-']");
            for (Element a : anchorsNoticias) {
                String abs = absolutizeUrl(homeUrl, a.attr("href"));
                if (isLikelyPost(abs)) {
                    enlaces.add(abs);
                }
            }
            return new ArrayList<>(enlaces);
        }

        // 👇 PARA OTROS SITIOS (como diariosinfronteras)
        // 1) Como tu script original: h3 > a
        Elements h3s = doc.select("h3");
        for (Element h3 : h3s) {
            Element a = h3.selectFirst("a[href]");
            if (a != null) {
                String abs = absolutizeUrl(homeUrl, a.attr("href"));
                if (isLikelyPost(abs)) enlaces.add(abs);
            }
        }

        // 2) Anclas que contienen año en la ruta (clásico /2024/05/10/)
        Elements anchors = doc.select("a[href*='/20']");
        for (Element a : anchors) {
            String abs = absolutizeUrl(homeUrl, a.attr("href"));
            if (isLikelyPost(abs)) enlaces.add(abs);
        }

        return new ArrayList<>(enlaces);
    }


    /** Extrae detalle de una noticia */
    private static Articulo extractArticle(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .referrer("https://www.google.com/")
                .get();

        Articulo a = new Articulo();
        a.url = url;

        // Título
        Element title = firstNonNull(
                doc.selectFirst("h1.entry-title"),
                doc.selectFirst("h1.post-title"),
                doc.selectFirst("h1.td-post-title"),
                doc.selectFirst("h1")
        );
        a.titulo = title != null ? cleanText(title.text()) : null;

        // Autor
        Element author = firstNonNull(
                doc.selectFirst(".author a"),
                doc.selectFirst(".td-post-author-name"),
                doc.selectFirst(".byline .author"),
                doc.selectFirst("meta[name=author]")
        );
        a.autor = author != null
                ? ("meta".equalsIgnoreCase(author.tagName()) ? author.attr("content") : cleanText(author.text()))
                : null;

        // Fecha (time[datetime] | JSON-LD | og:article:published_time)
        LocalDateTime fecha = null;
        Element timeEl = doc.selectFirst("time[datetime]");
        if (timeEl != null) {
            fecha = parseIsoDatetimeOrNull(timeEl.attr("datetime"));
        }
        if (fecha == null) {
            // og:article:published_time
            Element ogPub = doc.selectFirst("meta[property=article:published_time]");
            if (ogPub != null) fecha = parseIsoDatetimeOrNull(ogPub.attr("content"));
        }
        if (fecha == null) {
            // JSON-LD (datePublished)
            fecha = tryJsonLdDatePublished(doc);
        }
        a.fechaPublicado = fecha;

        // Imagen principal (varios intentos)
        a.imagenUrl = extractMainImageUrl(doc, url);

        // Contenido
        Element content = firstNonNull(
                doc.selectFirst("article .entry-content"),
                doc.selectFirst(".td-post-content"),
                doc.selectFirst(".post-content"),
                doc.selectFirst("article")
        );
        if (content != null) {
            List<String> partes = new ArrayList<>();
            Elements blocks = content.select("p, h2, h3, li");
            for (Element b : blocks) {
                String txt = cleanText(b.text());
                if (txt != null && !txt.isEmpty()) partes.add(txt);
            }
            a.contenido = partes.isEmpty() ? null : String.join("\n", partes);
        }

        // Tags
        List<String> tags = new ArrayList<>();
        for (Element e : doc.select("a[rel=tag]")) {
            String t = cleanText(e.text());
            if (t != null && !t.isEmpty()) tags.add(t);
        }
        a.tags = tags.isEmpty() ? null : String.join(", ", tags);

        // Categorías
        List<String> categorias = new ArrayList<>();
        for (Element e : doc.select(".td-post-category a, .cat-links a, a[rel=category]")) {
            String c = cleanText(e.text());
            if (c != null && !c.isEmpty()) categorias.add(c);
        }
        a.categorias = categorias.isEmpty() ? null : String.join(", ", categorias);

        return a;
    }

    /** Intenta múltiples fuentes para la imagen principal */
    private static String extractMainImageUrl(Document doc, String pageUrl) {
        // 1) og:image
        Element ogImg = doc.selectFirst("meta[property=og:image], meta[name=og:image]");
        if (ogImg != null && notBlank(ogImg.attr("content"))) return absolutizeUrl(pageUrl, ogImg.attr("content"));

        // 2) twitter:image
        Element twImg = doc.selectFirst("meta[name=twitter:image], meta[property=twitter:image]");
        if (twImg != null && notBlank(twImg.attr("content"))) return absolutizeUrl(pageUrl, twImg.attr("content"));

        // 3) featured image típica WP
        Element wpImg = doc.selectFirst("img.wp-post-image");
        if (wpImg != null && notBlank(wpImg.attr("src"))) return absolutizeUrl(pageUrl, wpImg.attr("src"));

        // 4) figure > img
        Element figImg = doc.selectFirst("figure img");
        if (figImg != null && notBlank(figImg.attr("src"))) return absolutizeUrl(pageUrl, figImg.attr("src"));

        // 5) primera imagen dentro del contenido
        Element contentFirstImg = doc.selectFirst("article .entry-content img, .td-post-content img, .post-content img");
        if (contentFirstImg != null && notBlank(contentFirstImg.attr("src"))) return absolutizeUrl(pageUrl, contentFirstImg.attr("src"));

        return null;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** Intenta datePublished desde JSON-LD */
    private static LocalDateTime tryJsonLdDatePublished(Document doc) {
        try {
            for (Element script : doc.select("script[type=application/ld+json]")) {
                String json = script.data();
                // búsqueda simple: "datePublished":"..."
                int idx = json.indexOf("\"datePublished\"");
                if (idx < 0) continue;
                int colon = json.indexOf(':', idx);
                if (colon < 0) continue;
                int q1 = json.indexOf('"', colon + 1);
                if (q1 < 0) continue;
                int q2 = json.indexOf('"', q1 + 1);
                if (q2 < 0) continue;
                String value = json.substring(q1 + 1, q2).trim();
                LocalDateTime parsed = parseIsoDatetimeOrNull(value);
                if (parsed != null) return parsed;
            }
        } catch (Exception ignore) { }
        return null;
    }

    // ================== Persistencia ==================
    private static void saveArticle(Articulo art) throws SQLException {
        String sql = """
            INSERT INTO articulos
              (url, titulo, autor, fecha_publicado, imagen_url, contenido, tags, categorias)
            VALUES (?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
              titulo=VALUES(titulo),
              autor=VALUES(autor),
              fecha_publicado=VALUES(fecha_publicado),
              imagen_url=VALUES(imagen_url),
              contenido=VALUES(contenido),
              tags=VALUES(tags),
              categorias=VALUES(categorias),
              actualizado_en=CURRENT_TIMESTAMP
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, art.url);
            ps.setString(2, art.titulo);
            ps.setString(3, art.autor);
            if (art.fechaPublicado != null) {
                ps.setTimestamp(4, Timestamp.valueOf(art.fechaPublicado));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setString(5, art.imagenUrl);
            ps.setString(6, art.contenido);
            ps.setString(7, art.tags);
            ps.setString(8, art.categorias);

            ps.executeUpdate();
        }
    }
    // Diccionario de categorías -> lista de palabras clave
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
            "Política", List.of("congreso", "presidente", "ministro", "gobierno", "elecciones"),
            "Deportes", List.of("fútbol", "gol", "liga", "mundial", "melgar", "alianza", "cristal"),
            "Economía", List.of("dólar", "sunat", "inflación", "mercado", "economía"),
            "Tecnología", List.of("app", "software", "intel", "google", "facebook", "redes sociales"),
            "Cultura", List.of("festival", "arte", "cine", "literatura", "música"),
            "Mundo", List.of("guerra", "eeuu", "china", "rusia", "internacional", "onu"),
            "Farandula", List.of("escándalo", "farándula", "espectáculos", "gisela", "famoso"),
            "Crimen", List.of("asesinato", "robo", "violación", "policía", "capturan", "homicidio")
    );
    private static String inferCategoria(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return "Otros";
        }

        String tituloLower = titulo.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (tituloLower.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }

        return "Otros"; // si no hay coincidencia
    }

}
