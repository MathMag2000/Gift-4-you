package com.gift4you.integracao;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encontra no HTML de uma página de produto a imagem que a própria loja declara para compartilhamento
 * (Open Graph, Twitter Card, link image_src) ou nos dados estruturados do produto (JSON-LD).
 */
public class LeitorImagemHtml {

    private static final List<String> METAS_DE_IMAGEM = List.of(
            "og:image:secure_url", "og:image", "og:image:url", "twitter:image", "twitter:image:src");

    private static final Pattern TAG_META = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_LINK = Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATRIBUTO = Pattern.compile(
            "([a-zA-Z_:.-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
    private static final Pattern JSON_LD = Pattern.compile(
            "<script[^>]*type\\s*=\\s*[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ENTIDADE = Pattern.compile("&(#x?[0-9a-fA-F]+|amp|quot|apos|lt|gt);");

    public Optional<String> encontrarImagem(String html, URI enderecoPagina) {
        return imagemDasMetas(html)
                .or(() -> imagemDoLink(html))
                .or(() -> imagemDoJsonLd(html))
                .flatMap(endereco -> resolver(endereco, enderecoPagina));
    }

    private Optional<String> imagemDasMetas(String html) {
        Map<String, String> metas = new HashMap<>();
        Matcher tag = TAG_META.matcher(html);
        while (tag.find()) {
            Map<String, String> atributos = atributos(tag.group());
            String chave = atributos.getOrDefault("property", atributos.get("name"));
            String conteudo = atributos.get("content");
            if (chave != null && conteudo != null && !conteudo.isBlank()) {
                metas.putIfAbsent(chave.toLowerCase(Locale.ROOT), conteudo);
            }
        }
        return METAS_DE_IMAGEM.stream().map(metas::get).filter(valor -> valor != null).findFirst();
    }

    private Optional<String> imagemDoLink(String html) {
        Matcher tag = TAG_LINK.matcher(html);
        while (tag.find()) {
            Map<String, String> atributos = atributos(tag.group());
            if ("image_src".equalsIgnoreCase(atributos.get("rel")) && atributos.get("href") != null) {
                return Optional.of(atributos.get("href"));
            }
        }
        return Optional.empty();
    }

    private Optional<String> imagemDoJsonLd(String html) {
        Matcher script = JSON_LD.matcher(html);
        while (script.find()) {
            try {
                Optional<String> imagem = procurarImagem(Json.ler(script.group(1).trim()));
                if (imagem.isPresent()) {
                    return imagem;
                }
            } catch (IllegalArgumentException e) {
                // JSON-LD malformado: tenta o próximo bloco.
            }
        }
        return Optional.empty();
    }

    /** Procura o campo "image" em qualquer nível do JSON-LD, priorizando o primeiro encontrado. */
    private Optional<String> procurarImagem(Object valor) {
        if (valor instanceof Map<?, ?> objeto) {
            Optional<String> imagem = textoDeImagem(objeto.get("image"));
            if (imagem.isPresent()) {
                return imagem;
            }
            for (Object filho : objeto.values()) {
                imagem = procurarImagem(filho);
                if (imagem.isPresent()) {
                    return imagem;
                }
            }
        } else if (valor instanceof List<?> lista) {
            for (Object item : lista) {
                Optional<String> imagem = procurarImagem(item);
                if (imagem.isPresent()) {
                    return imagem;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> textoDeImagem(Object imagem) {
        if (imagem instanceof String texto && !texto.isBlank()) {
            return Optional.of(texto);
        }
        if (imagem instanceof List<?> lista && !lista.isEmpty()) {
            return textoDeImagem(lista.getFirst());
        }
        if (imagem instanceof Map<?, ?> objeto) {
            return textoDeImagem(objeto.get("url")).or(() -> textoDeImagem(objeto.get("contentUrl")));
        }
        return Optional.empty();
    }

    private Map<String, String> atributos(String tag) {
        Map<String, String> atributos = new HashMap<>();
        Matcher atributo = ATRIBUTO.matcher(tag);
        while (atributo.find()) {
            String valor = atributo.group(2) != null ? atributo.group(2)
                    : atributo.group(3) != null ? atributo.group(3) : atributo.group(4);
            atributos.putIfAbsent(atributo.group(1).toLowerCase(Locale.ROOT), decodificarEntidades(valor.trim()));
        }
        return atributos;
    }

    private String decodificarEntidades(String texto) {
        Matcher entidade = ENTIDADE.matcher(texto);
        StringBuilder resultado = new StringBuilder();
        while (entidade.find()) {
            String nome = entidade.group(1);
            String substituto = switch (nome) {
                case "amp" -> "&";
                case "quot" -> "\"";
                case "apos" -> "'";
                case "lt" -> "<";
                case "gt" -> ">";
                default -> caractereNumerico(nome).orElse(entidade.group());
            };
            entidade.appendReplacement(resultado, Matcher.quoteReplacement(substituto));
        }
        entidade.appendTail(resultado);
        return resultado.toString();
    }

    /** Converte entidades como &#47; e &#x2F; no caractere correspondente. */
    private Optional<String> caractereNumerico(String nome) {
        try {
            boolean hexadecimal = nome.length() > 1 && (nome.charAt(1) == 'x' || nome.charAt(1) == 'X');
            int codigo = hexadecimal ? Integer.parseInt(nome.substring(2), 16) : Integer.parseInt(nome.substring(1));
            return Optional.of(new String(Character.toChars(codigo)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Converte endereços relativos (ex.: /img/produto.jpg) e aceita somente http e https. */
    private Optional<String> resolver(String endereco, URI enderecoPagina) {
        try {
            URI resolvido = enderecoPagina.resolve(endereco.trim().replace(" ", "%20"));
            String esquema = resolvido.getScheme();
            if (esquema != null && (esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))) {
                return Optional.of(resolvido.toString());
            }
        } catch (IllegalArgumentException e) {
            // Endereço inválido: considera que a página não informou imagem.
        }
        return Optional.empty();
    }
}
