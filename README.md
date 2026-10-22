# 🎁 Gift 4 You

Sistema web de sugestões de presentes personalizados. Você cadastra a pessoa (idade, vínculo, gostos e
interesses, o que ela não gosta, ocasião e orçamento) e o sistema sugere presentes do catálogo ou gera
ideias com IA, com link de compra. As sugestões podem ser favoritadas e ficam guardadas no histórico.

## Requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| [Java (JDK)](https://adoptium.net/) | **25** | Obrigatório. |
| [Git](https://git-scm.com/) | qualquer | Para baixar o projeto. |
| Maven | — | **Não precisa instalar**: o projeto já traz o Maven Wrapper (`mvnw`). |
| Chave da API Gemini | — | Opcional. Só é necessária para o botão **Ideias com IA**. |

Para conferir o Java instalado:

```bash
java -version
```

A variável de ambiente `JAVA_HOME` deve apontar para a pasta do JDK 25. Sem ela, o `mvnw` mostra o erro
*"The JAVA_HOME environment variable is not defined correctly"*.

## Instalação

```bash
git clone https://github.com/MathMag2000/Gift-4-you.git
cd Gift-4-you
```

## Executando

**Windows (PowerShell ou CMD):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

Na primeira execução o Maven baixa as dependências, o que pode levar alguns minutos.
Quando aparecer `Tomcat started on port 8080`, abra no navegador:

**http://localhost:8080**

Para parar o servidor, pressione `Ctrl + C` no terminal.

### Ideias com IA (opcional)

O botão **Ideias com IA** usa a API Gemini do Google. Sem a chave, o restante do sistema funciona
normalmente e o botão apenas avisa que a IA não está configurada.

Basta configurar a chave **uma vez** em cada computador:

1. Crie uma chave gratuita em <https://aistudio.google.com/apikey>.
2. Na pasta do projeto, copie o arquivo `chave-ia.properties.exemplo` com o nome **`chave-ia.properties`**:

   **Windows (PowerShell):**
   ```powershell
   Copy-Item chave-ia.properties.exemplo chave-ia.properties
   ```

   **Linux / macOS:**
   ```bash
   cp chave-ia.properties.exemplo chave-ia.properties
   ```

3. Abra `chave-ia.properties` e troque `COLE_SUA_CHAVE_AQUI` pela sua chave:

   ```properties
   gemini.chave-api=sua-chave-aqui
   ```

4. Inicie (ou reinicie) o servidor normalmente. A partir daí a IA funciona sempre que o projeto rodar.

O arquivo `chave-ia.properties` é ignorado pelo Git, então a chave **não vai para o GitHub**. Não coloque a
chave em nenhum outro arquivo do projeto: o repositório é público, e chaves publicadas são copiadas e
bloqueadas rapidamente. Para compartilhar a chave com a equipe, envie-a por um canal privado.

Também é possível usar a variável de ambiente `GEMINI_API_KEY` no lugar do arquivo. Se os dois existirem,
vale o arquivo. Opcionalmente, `GEMINI_MODEL` (ou `gemini.modelo` no arquivo) escolhe outro modelo do Gemini.

### Gerando um arquivo executável (.jar)

```bash
./mvnw package          # no Windows: .\mvnw.cmd package
java -jar target/gift-4-you-0.1.0.jar
```

### Rodando pelo IntelliJ IDEA

Abra a pasta do projeto (o IntelliJ reconhece o `pom.xml`), configure o SDK do projeto como Java 25 e
execute a classe `com.gift4you.Gift4YouApplication`. A chave da IA é lida do mesmo arquivo
`chave-ia.properties`, desde que o diretório de trabalho seja a pasta do projeto (o padrão do IntelliJ).

## Testes

```bash
./mvnw test             # no Windows: .\mvnw.cmd test
```

## Problemas comuns

| Problema | Solução |
|---|---|
| `JAVA_HOME environment variable is not defined correctly` | Defina `JAVA_HOME` com a pasta do JDK 25 e abra um novo terminal. |
| `Port 8080 was already in use` | Feche o outro programa que usa a porta ou rode em outra: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`. |
| `release version 25 not supported` | O Java instalado é mais antigo. Instale o JDK 25. |
| "IA não configurada" ao gerar ideias | Crie o arquivo `chave-ia.properties` na pasta do projeto (veja acima) e reinicie o servidor. |
| "O Gemini recusou a chave de API" | Confira se a chave em `chave-ia.properties` foi copiada inteira, sem espaços. |

## Observações

- Os dados ficam **em memória**: ao reiniciar o servidor, pessoas, presentes, favoritos e histórico são apagados.
- As ideias da IA são procuradas na Amazon para exibir o produto, o preço e a foto. Se a Amazon bloquear a
  consulta, a ideia aparece com o preço estimado e um link de busca.

## Estrutura do projeto

```
src/main/java/com/gift4you/
├── web/          controladores da API REST
├── service/      regras de negócio e validações
├── model/        entidades e enums
├── repository/   armazenamento dos dados
├── integracao/   Gemini, Amazon e leitura de páginas das lojas
├── config/       montagem dos objetos da aplicação
└── exception/    exceções do sistema
src/main/resources/static/   site (HTML, CSS e JavaScript)
src/test/                    testes automatizados
```
