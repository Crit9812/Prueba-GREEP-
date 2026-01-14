package Compartido.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodigoPostalService {

    private static final String BASE_URL = "https://api-sepomex.hckdrk.mx/query";
    private static final String TOKEN = "pruebas";

    public DireccionCp obtenerDireccion(String cp) throws IOException {
        String infoJson = request(BASE_URL + "/info_cp/" + cp + "?token=" + TOKEN);
        String coloniasJson = request(BASE_URL + "/get_colonia_por_cp/" + cp + "?token=" + TOKEN);

        List<String> colonias = extraerLista(coloniasJson, "colonia");
        if (colonias.isEmpty()) {
            colonias = extraerLista(infoJson, "asentamiento");
        }
        if (colonias.isEmpty()) {
            colonias = extraerLista(infoJson, "colonia");
        }

        String pais = extraerValor(infoJson, "pais");
        String estado = extraerValor(infoJson, "estado");
        String ciudad = extraerValor(infoJson, "ciudad");
        String municipio = extraerValor(infoJson, "municipio");
        String localidad = extraerValor(infoJson, "localidad");

        if (localidad == null || localidad.isBlank()) {
            localidad = municipio;
        }
        if (ciudad == null || ciudad.isBlank()) {
            ciudad = municipio;
        }

        return new DireccionCp(
                pais != null ? pais : "",
                estado != null ? estado : "",
                localidad != null ? localidad : "",
                ciudad != null ? ciudad : "",
                colonias
        );
    }

    private String request(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private List<String> extraerLista(String json, String clave) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        Set<String> resultados = new LinkedHashSet<>();
        Pattern arrayPattern = Pattern.compile("\"" + Pattern.quote(clave) + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher arrayMatcher = arrayPattern.matcher(json);
        if (arrayMatcher.find()) {
            String contenido = arrayMatcher.group(1);
            Matcher valores = Pattern.compile("\"(.*?)\"").matcher(contenido);
            while (valores.find()) {
                resultados.add(decodeJsonString(valores.group(1)));
            }
        }

        if (resultados.isEmpty()) {
            Pattern valuePattern = Pattern.compile("\"" + Pattern.quote(clave) + "\"\\s*:\\s*\"(.*?)\"");
            Matcher matcher = valuePattern.matcher(json);
            while (matcher.find()) {
                resultados.add(decodeJsonString(matcher.group(1)));
            }
        }

        return new ArrayList<>(resultados);
    }

    private String extraerValor(String json, String clave) {
        if (json == null || json.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile("\"" + Pattern.quote(clave) + "\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return decodeJsonString(matcher.group(1));
        }
        return null;
    }

    private String decodeJsonString(String value) {
        if (value == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case '/':
                        sb.append('/');
                        i++;
                        break;
                    case 'b':
                        sb.append('\b');
                        i++;
                        break;
                    case 'f':
                        sb.append('\f');
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case 'u':
                        if (i + 5 < value.length()) {
                            String hex = value.substring(i + 2, i + 6);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 5;
                            } catch (NumberFormatException ex) {
                                sb.append("\\u").append(hex);
                                i += 5;
                            }
                        } else {
                            sb.append(next);
                            i++;
                        }
                        break;
                    default:
                        sb.append(next);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public record DireccionCp(String pais, String estado, String localidad, String ciudad, List<String> colonias) {}
}
