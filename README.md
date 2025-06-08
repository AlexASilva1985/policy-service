# 🛡️ Insurance Policy Service

## 📋 Visão Geral

O **Policy Service** é um microsserviço para gerenciamento de apólices de seguro, desenvolvido com **Spring Boot 3** e seguindo os princípios de **Clean Architecture**, **SOLID** e **Domain-Driven Design (DDD)**. 

O sistema oferece funcionalidades completas para criação, validação, análise de fraude, processamento de pagamentos e aprovação de apólices de seguro.

## 🏗️ Arquitetura

### Clean Architecture

O projeto segue a **Clean Architecture** com camadas bem definidas:

```
┌─────────────────────┐
│    Controllers      │ ← Interface Layer (Adapters)
├─────────────────────┤
│    Use Cases        │ ← Application Layer (Services)
├─────────────────────┤
│    Domain Models    │ ← Domain Layer (Entities, Value Objects)
└─────────────────────┘
│   Infrastructure    │ ← Infrastructure Layer (Database, External APIs)
└─────────────────────┘
```

### 📁 Estrutura de Diretórios

```
src/main/java/com/insurance/
├── controller/          # REST Controllers (Interface Layer)
├── service/            # Business Logic (Application Layer)
│   ├── impl/          # Service Implementations
├── domain/            # Domain Models (Domain Layer)
│   ├── enums/        # Domain Enums
├── dto/               # Data Transfer Objects
├── repository/        # Data Access Interfaces
├── infrastructure/    # External Dependencies
│   ├── messaging/    # RabbitMQ Configuration
│   └── config/       # Spring Configurations
├── event/            # Domain Events
└── mapper/           # Object Mappers
```

## 🚀 Tecnologias

- **Java 17**
- **Spring Boot 3.2.3**
- **Spring Data JPA**
- **PostgreSQL**
- **RabbitMQ** (Messaging)
- **Docker & Docker Compose**
- **Maven**
- **JUnit 5** & **Mockito** (Testes)
- **JaCoCo** (Code Coverage)
- **Spring Actuator** (Observabilidade)

## 📊 Funcionalidades

### 🏷️ Gestão de Apólices
- ✅ Criação de solicitações de apólice
- ✅ Validação de regras de negócio por categoria e tipo de cliente
- ✅ Análise de fraude integrada
- ✅ Processamento de pagamentos
- ✅ Aprovação/Rejeição de apólices
- ✅ Cancelamento de apólices

### 🔍 Análise de Risco e Fraude
- **Classificação de Clientes**: REGULAR, HIGH_RISK, PREFERRED, NO_INFORMATION
- **Categorias de Seguro**: LIFE, AUTO, RESIDENTIAL, TRAVEL, HEALTH
- **Limites por Categoria**: Validação automática baseada no tipo de cliente
- **Detecção de Ocorrências**: HIGH_VALUE, EXTREME_VALUE

### 📨 Sistema de Eventos
- Notificações assíncronas via RabbitMQ
- Eventos de domínio para auditoria
- Integração com sistemas externos

## 🛠️ Quick Start

### Pré-requisitos
- Docker & Docker Compose
- Java 17+ (para desenvolvimento local)
- Maven 3.8+

### 🐳 Executando com Docker

```bash
# Clone o repositório
git clone https://github.com/AlexASilva1985/policy-service
cd policy-service

# Execute todos os serviços
docker-compose up -d

# Verificar logs
docker-compose logs -f app
```

### 💻 Desenvolvimento Local

```bash
# Instalar dependências
mvn clean install

# Executar testes
mvn test

# Executar aplicação
mvn spring-boot:run
```

## 📚 APIs Disponíveis

### 🔗 Endpoints Principais

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/policies` | Criar nova solicitação |
| `GET` | `/api/policies/{id}` | Buscar por ID |
| `GET` | `/api/policies/customer/{customerId}` | Buscar por cliente |
| `PUT` | `/api/policies/{id}/status` | Atualizar status |
| `POST` | `/api/policies/{id}/validate` | Validar apólice |
| `POST` | `/api/policies/{id}/fraud-analysis` | Análise de fraude |
| `POST` | `/api/policies/{id}/payment` | Processar pagamento |
| `POST` | `/api/policies/{id}/subscription` | Processar assinatura |
| `DELETE` | `/api/policies/{id}` | Cancelar apólice |

### 📋 Exemplo de Request

```json
{
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "productId": "660f9500-f30c-52e5-b827-557766551111",
  "category": "AUTO",
  "salesChannel": "MOBILE",
  "paymentMethod": "CREDIT_CARD",
  "totalMonthlyPremiumAmount": 500.00,
  "insuredAmount": 150000.00,
  "coverages": {
    "COLLISION": 50000.00,
    "COMPREHENSIVE": 30000.00
  }
}
```

## 🧪 Testes e Qualidade

### Coverage Report
```bash
# Executar testes com coverage
mvn clean test

# Ver relatório HTML
open target/jacoco/index.html
```

### 📊 Métricas de Qualidade
- **Cobertura de Testes**: 85%+
- **Testes Unitários**: 100% dos serviços
- **Testes de Integração**: Controllers e Repository
- **Validação de Contratos**: DTOs e Domain Models

## 📈 Observabilidade

### Health Checks
```bash
curl http://localhost:8080/actuator/health
```

### Métricas
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

### Logs Estruturados
- **Nível**: DEBUG para desenvolvimento, INFO para produção
- **Formato**: JSON estruturado
- **Rastreamento**: Request ID para debugging

## 🗃️ Banco de Dados
postgrel

### Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `SPRING_DATASOURCE_URL` | URL do PostgreSQL | `jdbc:postgresql://localhost:5432/insurance_db` |
| `SPRING_RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `FRAUD_API_URL` | URL da API de Fraude | `http://localhost:1080/api/fraud-analysis` |

### Profiles

- **`default`**: Desenvolvimento local
- **`docker`**: Ambiente containerizado
- **`test`**: Execução de testes

## 📋 Design Patterns Utilizados

1. **Repository Pattern** - Abstração de acesso a dados
2. **Factory Pattern** - Criação de responses padronizados
3. **Strategy Pattern** - Validação por tipo de cliente
4. **Observer Pattern** - Sistema de eventos
5. **Builder Pattern** - Construção de objetos complexos
6. **Dependency Injection** - Inversão de dependências

## 🎯 Princípios SOLID

- ✅ **Single Responsibility**: Cada classe tem uma única responsabilidade
- ✅ **Open/Closed**: Extensível para novos tipos sem modificação
- ✅ **Liskov Substitution**: Interfaces bem definidas
- ✅ **Interface Segregation**: Interfaces específicas e coesas
- ✅ **Dependency Inversion**: Dependências abstraídas via interfaces
