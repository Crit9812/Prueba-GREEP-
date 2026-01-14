package Compartido.helper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodigoPostalService {
    private static final String URL_TEMPLATE = "https://api-sepomex.hckdrk.mx/query/info_cp/%s";
    private static final Pattern LIST_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern VALUE_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CompletableFuture<Optional<CodigoPostalInfo>> buscarCodigoPostal(String cp) {
        if (cp == null || !cp.matches("\\d{5}")) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(Locale.ROOT, URL_TEMPLATE, cp)))
                .GET()
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        return Optional.<CodigoPostalInfo>empty();
                    }
                    return parseCodigoPostal(response.body());
                })
                .exceptionally(error -> Optional.empty());
    }

    private Optional<CodigoPostalInfo> parseCodigoPostal(String body) {
        String pais = extraerCampo(body, "pais");
        String estado = extraerCampo(body, "estado");
        String ciudad = extraerCampo(body, "ciudad");
        String localidad = extraerCampo(body, "localidad");
        if (localidad.isBlank()) {
            localidad = extraerCampo(body, "municipio");
        }

        List<String> colonias = extraerLista(body, "asentamiento");
        if (colonias.isEmpty()) {
            String coloniaUnica = extraerCampo(body, "asentamiento");
            if (!coloniaUnica.isBlank()) {
                colonias = List.of(coloniaUnica);
            }
        }

        if (pais.isBlank() && estado.isBlank() && ciudad.isBlank() && localidad.isBlank() && colonias.isEmpty()) {
            return Optional.empty();
        }

        if (pais.isBlank()) {
            pais = "México";
        }

        return Optional.of(new CodigoPostalInfo(pais, estado, localidad, ciudad, colonias));
    }

    private String extraerCampo(String body, String campo) {
        Pattern pattern = Pattern.compile(String.format(VALUE_PATTERN.pattern(), Pattern.quote(campo)), Pattern.DOTALL);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return limpiarValor(matcher.group(1));
        }
        return "";
    }

    private List<String> extraerLista(String body, String campo) {
        Pattern pattern = Pattern.compile(String.format(LIST_PATTERN.pattern(), Pattern.quote(campo)), Pattern.DOTALL);
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return Collections.emptyList();
        }

        String contenido = matcher.group(1);
        Matcher itemMatcher = Pattern.compile("\"(.*?)\"").matcher(contenido);
        List<String> valores = new ArrayList<>();
        while (itemMatcher.find()) {
            String valor = limpiarValor(itemMatcher.group(1));
            if (!valor.isBlank()) {
                valores.add(valor);
            }
        }
        return valores;
    }

    private String limpiarValor(String valor) {
        return valor.replace("\\\"", "\"").replace("\\\\", "\\").trim();
    }

    public static class CodigoPostalInfo {
        private final String pais;
        private final String estado;
        private final String localidad;
        private final String ciudad;
        private final List<String> colonias;

        public CodigoPostalInfo(String pais, String estado, String localidad, String ciudad, List<String> colonias) {
            this.pais = pais;
            this.estado = estado;
            this.localidad = localidad;
            this.ciudad = ciudad;
            this.colonias = colonias == null ? Collections.emptyList() : List.copyOf(colonias);
        }

        public String getPais() {
            return pais;
        }

        public String getEstado() {
            return estado;
        }

        public String getLocalidad() {
            return localidad;
        }

        public String getCiudad() {
            return ciudad;
        }

        public List<String> getColonias() {
            return colonias;
        }
    }
}
