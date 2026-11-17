# Zelia Agenda

API REST para o gerenciamento de usuários, turmas e agenda de aulas do Grupo Escolar Zélia Milles.

Com ela é possível cadastrar professores e secretários, organizar turmas, reservar horários de aula por período e data e recuperar senhas por e-mail.

## Tecnologias

As informações abaixo foram inferidas do código-fonte:

- Java 17 ou superior (uso de `record`, text blocks e `Stream.toList()`)
- Spring Boot com Spring Web, Spring Security, Spring Data JPA, Spring Mail e Bean Validation
- JWT com a biblioteca `com.auth0:java-jwt` (HMAC256)
- BCrypt para hash de senhas
- MySQL com migrações Flyway
- Lombok

O arquivo enviado não contém o `pom.xml` (ou `build.gradle`). Ajuste os comandos de build de acordo com o seu projeto.

## Estrutura do projeto

```
src/main/java/com/zelia/agenda/
├── controllers/      # AuthController, UserController, SchoolClassController, AgendaController
├── service/          # UserService, SchoolClassService, AgendaService, EmailService
├── domain/
│   ├── user/         # User, UserType, PasswordPolicy, AdminInitializer, DTOs
│   ├── schoolClass/  # SchoolClass, DTOs
│   └── agenda/       # Agenda, Period, ClassTime, DTOs
├── infra/
│   ├── security/     # SecurityConfiguration, TokenService (JWT)
│   ├── filters/      # JwtFilter
│   └── ratelimit/    # RateLimiter
└── exceptions/       # GlobalExceptionHandler e exceções de negócio
src/main/resources/
├── application.properties
└── db/migration/     # V1 a V6 (Flyway)
```

## Configuração

### Banco de dados

Crie o banco antes de subir a aplicação. O Flyway cria as tabelas automaticamente:

```sql
CREATE DATABASE agenda_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

O banco esperado está em `localhost:3306/agenda_db`.

### Variáveis de ambiente obrigatórias

A aplicação não sobe sem estas variáveis:

| Variável         | Descrição                                                                 |
|------------------|---------------------------------------------------------------------------|
| `MYSQL_USER`     | Usuário do MySQL                                                          |
| `MYSQL_PASSWORD` | Senha do MySQL                                                            |
| `JWT_SECRET`     | Segredo para assinar os tokens. Mínimo de 32 bytes aleatórios            |
| `ADMIN_EMAIL`    | E-mail do administrador criado na primeira execução                       |
| `ADMIN_PASSWORD` | Senha do administrador. Deve atender à política de senha (8 a 72 bytes)  |
| `APP_BASE_URL`   | URL do front-end, usada no link de recuperação (ex.: `https://agenda.exemplo.com`) |
| `EMAIL_USERNAME` | Conta usada para enviar e-mails (SMTP do Gmail, porta 587 com STARTTLS)  |
| `EMAIL_PASSWORD` | Senha da conta de e-mail (no Gmail, use uma senha de app)                |

Opcionalmente, `SHOW_SQL=true` mostra as queries SQL no log.

### Administrador inicial

Na primeira execução, `AdminInitializer` cria o usuário administrador com `ADMIN_EMAIL` e `ADMIN_PASSWORD`. Se a conta já existir, a aplicação só não altera nada. Se ela ainda tiver a senha padrão antiga (`admin`), uma mensagem de erro é registrada no log.

## Executando

1. Crie o banco `agenda_db`.
2. Defina as variáveis de ambiente listadas acima.
3. Execute a classe `com.zelia.agenda.AgendaApplication` pela sua IDE ou pelo comando de build do projeto.

A API fica disponível em `http://localhost:8080`.

## Autenticação

A API usa JWT no header `Authorization`.

1. Faça login em `POST /auth/login`. A resposta traz o token:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

2. Envie o token em todas as rotas protegidas:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Pontos importantes:

- O token expira em **2 horas**.
- O token é invalidado quando a conta é desativada, a senha é redefinida, o papel ou o e-mail do usuário é alterado, ou quando o usuário faz logout (`POST /auth/logout`).
- Papel e status são sempre lidos do banco a cada requisição, e não do token.

## Perfis e permissões

Existem três tipos de usuário (`userType`): `ADMIN`, `SECRETARIO` e `PROFESSOR`.

| Ação                                              | ADMIN | SECRETARIO | PROFESSOR |
|---------------------------------------------------|:-----:|:----------:|:---------:|
| Criar, editar e buscar qualquer usuário           |   ✅   |     —      |     —     |
| Listar usuários, desativar e reativar             |   ✅   |     ✅      |     —     |
| Ver os próprios dados de usuário                  |   ✅   |     ✅      |     ✅     |
| Criar, listar e excluir turmas                    |   ✅   |     ✅      |     —     |
| Criar e consultar agendamentos                    |   ✅   |     ✅      |     ✅     |
| Editar e excluir agendamentos                     | ✅ (todos) | ✅ (próprios) | ✅ (próprios) |

## Endpoints

### Autenticação

| Método | Rota           | Acesso        | Sucesso | Descrição                       |
|--------|----------------|---------------|---------|---------------------------------|
| POST   | `/auth/login`  | Público       | 200     | Autentica e retorna o token     |
| POST   | `/auth/logout` | Autenticado   | 204     | Invalida todos os tokens do usuário |

### Usuários

| Método | Rota                        | Acesso                       | Sucesso | Descrição                                          |
|--------|-----------------------------|------------------------------|---------|----------------------------------------------------|
| POST   | `/user`                     | ADMIN                        | 201     | Cria um usuário                                    |
| GET    | `/user`                     | ADMIN, SECRETARIO            | 200     | Lista apenas os usuários ativos                    |
| GET    | `/user/{id}`                | ADMIN, SECRETARIO ou o próprio | 200   | Busca um usuário                                   |
| PUT    | `/user/{id}`                | ADMIN                        | 200     | Atualiza nome, e-mail e tipo                       |
| DELETE | `/user/{id}`                | ADMIN, SECRETARIO            | 200     | Desativa o usuário (não exclui) e retorna os dados |
| PATCH  | `/user/reactive/{id}`       | ADMIN, SECRETARIO            | 200     | Reativa um usuário desativado                      |
| POST   | `/user/forgot-my-password`  | Público                      | 202     | Envia o e-mail com o link de recuperação           |
| POST   | `/user/recovery-account`    | Público                      | 204     | Redefine a senha usando o token do e-mail          |

### Turmas

| Método | Rota            | Acesso            | Sucesso | Descrição                              |
|--------|-----------------|-------------------|---------|----------------------------------------|
| GET    | `/class`        | Autenticado       | 200     | Lista todas as turmas                  |
| POST   | `/class`        | ADMIN, SECRETARIO | 200     | Cria uma turma                         |
| DELETE | `/class/{id}`   | ADMIN, SECRETARIO | 204     | Exclui uma turma sem agendamentos      |

### Agenda

| Método | Rota             | Acesso                    | Sucesso | Descrição                                  |
|--------|------------------|---------------------------|---------|--------------------------------------------|
| POST   | `/agenda`        | Autenticado               | 201     | Cria um agendamento para o usuário logado  |
| POST   | `/agenda/list`   | Autenticado               | 200     | Lista os agendamentos de um dia            |
| GET    | `/agenda/{id}`   | Autenticado               | 200     | Busca um agendamento                       |
| PUT    | `/agenda/{id}`   | Dono do registro ou ADMIN | 200     | Atualiza um agendamento                    |
| DELETE | `/agenda/{id}`   | Dono do registro ou ADMIN | 204     | Remove um agendamento                      |

## Exemplos de requisição

Os exemplos seguem as validações do código. Substitua os valores entre `< >`.

### Login

`POST /auth/login`

```json
{
  "email": "<email>",
  "password": "<senha>"
}
```

### Criar usuário

`POST /user`

```json
{
  "name": "Maria Silva",
  "email": "maria@exemplo.com",
  "password": "<senha com 8 a 72 caracteres>",
  "userType": "PROFESSOR"
}
```

### Atualizar usuário

`PUT /user/{id}`

```json
{
  "name": "Maria Silva",
  "email": "maria@exemplo.com",
  "userType": "SECRETARIO"
}
```

### Esqueci minha senha

`POST /user/forgot-my-password`

```json
{
  "email": "maria@exemplo.com"
}
```

O e-mail enviado contém um link no formato `{APP_BASE_URL}/recuperar-conta#codigo=<token>`. O token é o valor depois de `codigo=`.

### Redefinir senha

`POST /user/recovery-account`

```json
{
  "token": "<token recebido no e-mail>",
  "newPassword": "<nova senha com 8 a 72 caracteres>"
}
```

### Criar turma

`POST /class`

```json
{
  "year": "8",
  "number": "I"
}
```

A turma é salva com o nome `8° I`. Esse nome precisa ser único.

### Criar agendamento

`POST /agenda`

```json
{
  "schoolClassId": 1,
  "observation": "Prova de matemática",
  "period": "MATUTINO",
  "classTime": "AULA_1",
  "date": "2026-11-18"
}
```

Resposta (201):

```json
{
  "id": 7,
  "user": { "name": "Maria Silva" },
  "schoolClass": { "id": 1, "name": "8° I" },
  "observation": "Prova de matemática",
  "period": "MATUTINO",
  "classTime": "AULA_1",
  "date": "2026-11-18"
}
```

### Atualizar agendamento

`PUT /agenda/{id}`

Usa o mesmo corpo do `POST /agenda`.

### Listar agenda do dia

`POST /agenda/list`

```json
{
  "date": "2026-11-18"
}
```

Resposta (200). Os agendamentos são separados por período. Tudo que não é `MATUTINO` vai para `vespertino`.

```json
{
  "matutino": [ { "id": 7, "...": "..." } ],
  "vespertino": []
}
```

## Valores aceitos

| Campo       | Valores                                  |
|-------------|------------------------------------------|
| `userType`  | `ADMIN`, `SECRETARIO`, `PROFESSOR`       |
| `period`    | `MATUTINO`, `VESPERTINO`                 |
| `classTime` | `AULA_1`, `AULA_2`, `AULA_3`, `AULA_4`, `AULA_5` |
| `date`      | Formato `yyyy-MM-dd`                     |

## Regras de negócio

- **Horário único:** um período, aula e data só pode ter um agendamento, considerando todas as turmas. Essa regra também existe como constraint no banco (`uk_agenda_period_class_time_date`).
- **Turma com agendamentos não pode ser excluída** (409).
- **Nome da turma é único** e é montado como `{year}° {number}`.
- **Usuário não pode desativar a própria conta.**
- **Deve existir pelo menos um administrador ativo.** Não é possível rebaixar nem desativar o último.
- **Usuário inativo não aparece na listagem** (`GET /user`) e não consegue fazer login.
- **Senha:** entre 8 e 72 bytes em UTF-8 (limite do BCrypt).
- **Token de recuperação:** válido por 15 minutos e de uso único. Ele é armazenado como hash SHA-256.
- **Recuperação de senha não revela se o e-mail existe:** a resposta é 202 em qualquer caso.
- **Mudança de senha, e-mail ou papel** invalida os tokens JWT já emitidos.

## Erros

Os erros seguem este formato:

```json
{
  "status": 409,
  "mensagem": "Já existe um agendamento para o período MATUTINO, horário de aula AULA_1 na data 2026-11-18",
  "timestamp": "2026-11-17T12:00:00Z"
}
```

Exceção: a falha de login retorna o texto simples `Email ou Senha invalidos`, com status 401.

| Status | Quando acontece                                                                 |
|--------|---------------------------------------------------------------------------------|
| 400    | Validação de campos, senha fora da política ou token de recuperação inválido/expirado |
| 401    | Token ausente, inválido, expirado ou revogado                                   |
| 403    | Usuário autenticado sem permissão para a ação                                   |
| 404    | Usuário, turma ou agendamento não encontrado                                    |
| 409    | E-mail ou turma já cadastrados, horário já ocupado, turma com agendamentos, regra de negócio violada |
| 429    | Limite de tentativas excedido                                                   |
| 500    | Erro inesperado                                                                 |

## Limites de tentativas

| Operação                  | Limite                                  |
|---------------------------|-----------------------------------------|
| Login                     | 100 por IP e 10 por e-mail a cada 15 minutos |
| Esqueci minha senha       | 10 por IP e 3 por e-mail por hora       |

Os contadores ficam em memória. Com mais de uma instância da aplicação, é preciso trocar por um armazenamento compartilhado, como o Redis.

## Coleção do Insomnia

O arquivo `Zelia_Agenda.yaml` contém as requisições de exemplo.

1. Abra o Insomnia.
2. Vá em **Application > Import** (ou **Import/Export > Import Data**).
3. Selecione o arquivo `Zelia_Agenda.yaml`.

As URLs apontam para `http://localhost:8080`. Crie uma variável de ambiente `base_url` e use `{{ base_url }}` nas rotas para trocar de ambiente com facilidade. Depois do login, copie o token para o campo de autenticação Bearer.

## Testes

O projeto tem testes unitários com JUnit 5, Mockito e AssertJ. As dependências vêm dos starters `*-test` do `pom.xml`, então não é preciso configurar nada extra.

### Como executar

```bash
mvn test
```

Você também pode rodar pela IDE, clicando com o botão direito na pasta `src/test/java`.

### O que está coberto

Os testes ficam em `src/test/java`, com a mesma estrutura de pacotes do código principal.

| Área | Arquivo | O que valida |
|------|---------|--------------|
| Serviço de usuários | `UserServiceTest` | criação, atualização, desativação, último admin, recuperação de senha e expiração do token |
| Serviço de agenda | `AgendaServiceTest` | horário ocupado, permissão de dono ou admin, lista separada por período |
| Serviço de turmas | `SchoolClassServiceTest` | formato do nome, duplicatas, exclusão com agendamentos vinculados |
| E-mail | `EmailServiceTest` | link de recuperação, escape de HTML, falha de SMTP sem propagar erro |
| Login | `AuthControllerTest` | login válido e inválido, conta inativa, limite de tentativas |
| JWT | `TokenServiceTest` | claims, expiração de 2 horas, tokens forjados ou de outro emissor |
| Filtro de autenticação | `JwtFilterTest` | sessão invalidada por desativação ou troca de versão |
| Validação dos DTOs | `DtoValidationTest` | regras do Bean Validation |
| Política de senha | `PasswordPolicyTest` | limite de 8 a 72 bytes em UTF-8 |
| Admin inicial | `AdminInitializerTest` | criação do admin e aviso de senha padrão |
| Rate limit | `RateLimiterTest` | limite por chave e reset da janela |
| Tratamento de erros | `GlobalExceptionHandlerTest` | status HTTP correto e erro 500 sem vazar detalhes |

Os testes de unidade não precisam de banco de dados. Os mocks substituem os repositórios, o `JavaMailSender` e as demais dependências.

### Teste de contexto

`AgendaApplicationTests` sobe a aplicação inteira. Por isso ele exige o MySQL rodando em `localhost:3306` e as variáveis de ambiente obrigatórias configuradas (veja a seção de configuração).

Para rodar só os testes de unidade, sem o contexto completo:

```bash
mvn test -Dtest='!AgendaApplicationTests'
```

## Segurança

- Troque a senha padrão `admin` assim que possível. A política de senha exige pelo menos 8 caracteres.
- Não versione credenciais, segredos JWT, senhas de e-mail nem tokens de recuperação. Use variáveis de ambiente.
- Não compartilhe tokens JWT. Um token válido dá acesso à conta até expirar ou ser revogado.
- Use HTTPS em produção. O `APP_BASE_URL` e a API devem ser servidos por TLS.
