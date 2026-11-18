# 🎁 Gift 4 You

Sistema web de sugestões de presentes personalizados. Você cadastra a pessoa (idade, vínculo, gostos e
interesses, o que ela não gosta, ocasião e orçamento) e o sistema sugere presentes do catálogo ou gera
ideias com IA, com link de compra. As sugestões podem ser favoritadas e ficam guardadas no histórico.

## Requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| [Java (JDK)](https://adoptium.net/) | **25** | Obrigatório. |
| [PostgreSQL](https://www.postgresql.org/download/) | 14 ou mais recente | Obrigatório. Guarda contas, pessoas, presentes, favoritos e histórico. |
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

## Banco de dados (PostgreSQL)

O sistema precisa de um banco PostgreSQL chamado `gift4you`. As **tabelas são criadas automaticamente** na
primeira execução (arquivo `src/main/resources/schema.sql`), e os dados existentes são mantidos nas seguintes.

**Opção 1: PostgreSQL instalado no computador**

1. Instale o PostgreSQL. Durante a instalação, anote a senha escolhida para o usuário `postgres`.
2. Crie o banco `gift4you`, pelo pgAdmin ou pelo terminal:

   ```bash
   psql -U postgres -c "CREATE DATABASE gift4you"
   ```

3. Se a senha do usuário `postgres` **não** for `postgres`, copie `banco.properties.exemplo` com o nome
   **`banco.properties`** e coloque a sua senha nele:

   ```powershell
   Copy-Item banco.properties.exemplo banco.properties     # Linux/macOS: cp banco.properties.exemplo banco.properties
   ```

   ```properties
   spring.datasource.password=sua-senha
   ```

   O arquivo `banco.properties` é ignorado pelo Git, então a senha **não vai para o GitHub**.

**Opção 2: Docker**

```bash
docker compose up -d
```

Sobe um PostgreSQL já com o banco `gift4you` e os dados padrão (usuário e senha `postgres`). Não é preciso
criar o `banco.properties`.

**Configuração padrão:** `jdbc:postgresql://localhost:5432/gift4you`, usuário `postgres`, senha `postgres`.
Também é possível usar as variáveis de ambiente `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. Se o arquivo
`banco.properties` existir, os valores dele têm prioridade.

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

### Conta de usuário

Ao abrir o site, crie uma conta (nome, e-mail e senha com pelo menos 8 caracteres, com letras e números) ou
entre com uma conta existente. Cada conta vê **somente os próprios dados**: pessoas, catálogo de presentes,
favoritos e histórico. O botão **Sair**, no menu lateral, encerra a sessão.

As senhas não são guardadas: o banco guarda apenas um hash PBKDF2-SHA256 com sal aleatório.

**Limites contra abuso** (os dados ficam na memória do servidor e zeram ao reiniciá-lo):

| O quê | Limite padrão | Propriedade para mudar |
|---|---|---|
| Pedidos de ideias com IA | 10 por hora, por conta | `gift4you.limite.ia-por-hora` |
| Senha errada no mesmo e-mail | 5 em 15 minutos; depois bloqueia o login desse e-mail por até 15 minutos | `gift4you.limite.login-falhas-por-email` |
| Senha errada vinda do mesmo endereço IP | 20 em 15 minutos | `gift4you.limite.login-falhas-por-ip` |
| Contas criadas pelo mesmo endereço IP | 10 por hora | `gift4you.limite.cadastros-por-ip-hora` |

As propriedades também podem ser definidas por variável de ambiente, como `GIFT4YOU_LIMITE_IA_POR_HORA=20`.

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

## Publicando o site

O projeto traz um `Dockerfile` pronto para hospedagens que aceitam Docker, como
[Railway](https://railway.com) e [Render](https://render.com). As duas oferecem PostgreSQL e HTTPS.

1. Crie um banco PostgreSQL na hospedagem e anote o servidor, a porta, o nome do banco, o usuário e a senha.
2. Crie o serviço web a partir do repositório do GitHub. A hospedagem encontra o `Dockerfile` e monta a imagem.
3. Defina as variáveis de ambiente no painel do serviço:

   | Variável | Exemplo | Obrigatória |
   |---|---|---|
   | `DB_URL` | `jdbc:postgresql://servidor:5432/gift4you` | sim |
   | `DB_USERNAME` | `gift4you` | sim |
   | `DB_PASSWORD` | a senha do banco | sim |
   | `GEMINI_API_KEY` | a chave da IA | não (sem ela, só o botão de IA fica indisponível) |

   Atenção ao formato de `DB_URL`: ele começa com `jdbc:postgresql://`. Muitas hospedagens mostram o endereço
   como `postgres://usuario:senha@servidor:5432/banco`. Nesse caso, use `jdbc:postgresql://servidor:5432/banco` em
   `DB_URL` e coloque o usuário e a senha nas outras duas variáveis.

4. Publique. Na primeira inicialização as tabelas são criadas sozinhas.

A imagem liga o perfil **`producao`** (`src/main/resources/application-producao.properties`), que:

- não inicia o servidor se faltar alguma variável do banco, e mostra qual falta;
- envia o cookie de login somente por HTTPS (por isso o site publicado precisa de HTTPS, que as hospedagens
  oferecem). Acessar por `http://` faz o login não funcionar;
- usa o endereço real de quem acessa, repassado pelo proxy da hospedagem, para os limites de login;
- não mostra detalhes internos nas páginas de erro.

Em todos os perfis, o servidor envia cabeçalhos de segurança: a política de conteúdo (CSP) só permite os
scripts do próprio site e impede que ele seja aberto dentro de outro site.

Para testar o perfil de produção no computador, sem Docker:

```powershell
$env:SPRING_PROFILES_ACTIVE="producao"; $env:DB_URL="jdbc:postgresql://localhost:5432/gift4you"
$env:DB_USERNAME="postgres"; $env:DB_PASSWORD="sua-senha"
.\mvnw.cmd package -DskipTests; java -jar target\gift-4-you-0.1.0.jar
```

O login não funciona em `http://localhost`, porque o cookie exige HTTPS. Isso é esperado.

## Testes

```bash
./mvnw test             # no Windows: .\mvnw.cmd test
```

Os testes de API usam um PostgreSQL embutido, baixado automaticamente pelo Maven: não precisam do banco
instalado nem de Docker.

## Problemas comuns

| Problema | Solução |
|---|---|
| `JAVA_HOME environment variable is not defined correctly` | Defina `JAVA_HOME` com a pasta do JDK 25 e abra um novo terminal. |
| `Port 8080 was already in use` | Feche o outro programa que usa a porta ou rode em outra: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`. |
| `release version 25 not supported` | O Java instalado é mais antigo. Instale o JDK 25. |
| "IA não configurada" ao gerar ideias | Crie o arquivo `chave-ia.properties` na pasta do projeto (veja acima) e reinicie o servidor. |
| "O Gemini recusou a chave de API" | Confira se a chave em `chave-ia.properties` foi copiada inteira, sem espaços. |
| `Connection to localhost:5432 refused` | O PostgreSQL não está rodando. Inicie o serviço do PostgreSQL ou rode `docker compose up -d`. |
| `password authentication failed for user "postgres"` | A senha do banco está errada. Coloque a senha certa em `banco.properties` (veja acima). |
| `database "gift4you" does not exist` | Crie o banco: `psql -U postgres -c "CREATE DATABASE gift4you"`. |

## Observações

- Os dados ficam no **PostgreSQL** e continuam lá quando o servidor reinicia. Já o login fica na memória do
  servidor: ao reiniciá-lo, é preciso entrar de novo.
- As ideias da IA são procuradas na Amazon para exibir o produto, o preço e a foto. Se a Amazon bloquear a
  consulta, a ideia aparece com o preço estimado e um link de busca.

## Estrutura do projeto

```
src/main/java/com/gift4you/
├── web/          controladores da API REST
├── service/      regras de negócio e validações
├── model/        entidades e enums
├── repository/   armazenamento dos dados (PostgreSQL via JDBC; versões em memória para os testes)
├── integracao/   Gemini, Amazon e leitura de páginas das lojas
├── config/       montagem dos objetos da aplicação
└── exception/    exceções do sistema
src/main/resources/static/   site (HTML, CSS e JavaScript, sem frameworks), no visual do wireframe do Figma
├── js/app.js                entrada do site: sessão, menu lateral e rotas (#/inicio, #/pessoas, ...)
├── js/telas/                uma tela por arquivo (início, pessoas, sugestões, favoritos, histórico, catálogo...)
└── js/componentes/          peças reutilizadas: cartões de produto, janela de detalhes, formulários, ícones
src/main/resources/schema.sql   tabelas do banco de dados
src/test/                    testes automatizados
```
