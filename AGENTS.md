# AGENTS.md

## Visão geral do projeto

Este projeto é um sistema de sugestões de presentes personalizado. O objetivo é auxiliar o usuário a encontrar presentes adequados para uma determinada pessoa com base em informações como idade, vínculo, gostos, interesses, coisas que não gosta, ocasião e faixa de orçamento.

## Tecnologias

* Java
* Programação Orientada a Objetos
* Git e GitHub
* Maven, caso utilizado pelo projeto

## Estrutura do projeto

O código-fonte deve permanecer dentro de `src/`.

As classes devem ser organizadas por responsabilidade, evitando concentrar toda a lógica em uma única classe.

## Regras de desenvolvimento

* Utilizar Java e os recursos disponíveis no projeto.
* Seguir princípios de Programação Orientada a Objetos.
* Utilizar nomes de classes, métodos e variáveis claros e descritivos.
* Evitar código duplicado.
* Cada classe deve possuir uma responsabilidade bem definida.
* Validar os dados informados pelo usuário.
* Manter a lógica de negócio separada da entrada e saída de dados sempre que possível.
* Não adicionar dependências desnecessárias.
* Não alterar funcionalidades existentes sem verificar o impacto nas demais partes do sistema.

## Funcionalidades principais

O sistema deverá permitir:

1. Cadastrar pessoas.
2. Alterar informações de pessoas.
3. Remover pessoas.
4. Consultar pessoas cadastradas.
5. Informar gostos e interesses.
6. Informar coisas que a pessoa não gosta.
7. Informar ocasião do presente.
8. Definir faixa de orçamento.
9. Gerar sugestões de presentes.
10. Favoritar sugestões.
11. Consultar histórico de sugestões.

## Modelo de dados

Uma pessoa pode possuir informações como:

* Nome
* Idade
* Vínculo
* Gostos
* Interesses
* Itens ou categorias que não gosta
* Ocasião
* Orçamento mínimo
* Orçamento máximo

Um presente pode possuir:

* Nome
* Categoria
* Descrição
* Preço
* Características
* Ocasiões compatíveis

## Sugestões

As sugestões devem considerar as informações cadastradas para a pessoa.

O sistema deve priorizar presentes que:

* estejam dentro da faixa de orçamento;
* sejam compatíveis com os gostos e interesses;
* sejam adequados para a ocasião;
* não estejam relacionados às categorias ou características rejeitadas pelo usuário.

## Git

Alterações devem ser registradas com commits claros e objetivos.

Exemplos:

* `feat: adiciona cadastro de pessoas`
* `feat: implementa sugestao de presentes`
* `fix: corrige validacao de orcamento`
* `docs: atualiza documentacao`

O código deve ser enviado para o repositório GitHub do projeto após as alterações relevantes.
