# 🎨 Visual Flow Diagrams (Mermaid)

## System Architecture Overview

```mermaid
graph TB
    subgraph Frontend
        UI[React Frontend<br/>Port: 5173]
    end
    
    subgraph "Microservices Layer"
        AS[Account Service<br/>:8081]
        IS[Item Service<br/>:8082]
        OS[Order Service<br/>:8083]
        PS[Payment Service<br/>:8084]
    end
    
    subgraph "Data Layer"
        MDB1[(MySQL<br/>Accounts)]
        MDB2[(MySQL<br/>Items)]
        CDB1[(Cassandra<br/>Orders)]
        CDB2[(Cassandra<br/>Payments)]
    end
    
    subgraph "Message Broker"
        K[Apache Kafka<br/>:9092]
        PE[payment.events]
        OE[order.events]
    end
    
    UI -->|REST + JWT| AS
    UI -->|REST + JWT| IS
    UI -->|REST + JWT| OS
    UI -->|REST + JWT| PS
    
    AS --> MDB1
    IS --> MDB2
    OS --> CDB1
    PS --> CDB2
    
    OS -->|Get Item Info| IS
    PS -->|Verify Order| OS
    OS -->|Deduct Inventory| IS
    
    PS -->|Publish| PE
    OS -->|Publish| OE
    K --> PE
    K --> OE
    PE -->|Consume| OS
    OE -->|Consume| PS
    
    style UI fill:#61dafb
    style AS fill:#90EE90
    style IS fill:#90EE90
    style OS fill:#90EE90
    style PS fill:#90EE90
    style K fill:#ff6b6b
```

## Payment & Order Completion Flow

```mermaid
sequenceDiagram
    participant F as Frontend
    participant PS as Payment Service
    participant OS as Order Service
    participant K as Kafka
    participant IS as Item Service
    participant DB as Cassandra
    
    F->>PS: POST /api/v1/payments<br/>{orderId, amount}
    activate PS
    
    PS->>OS: GET /api/v1/orders/{id}<br/>(Verify order exists)
    OS-->>PS: Order details
    
    PS->>DB: Save payment<br/>status=SUCCESS
    DB-->>PS: Saved
    
    PS->>K: Publish PaymentSucceeded<br/>{orderId, paymentId, amount}
    
    PS-->>F: 201 Created<br/>{paymentId, status}
    deactivate PS
    
    Note over K: Async Event Processing
    
    K->>OS: Consume PaymentSucceeded
    activate OS
    
    OS->>DB: Update order<br/>status=COMPLETED
    
    OS->>IS: PATCH /api/v1/items<br/>Deduct inventory
    IS-->>OS: Inventory updated
    
    deactivate OS
    
    Note over F,OS: Order is now COMPLETED<br/>Inventory deducted
```

## Order Cancellation & Refund Flow

```mermaid
sequenceDiagram
    participant F as Frontend
    participant OS as Order Service
    participant K as Kafka
    participant PS as Payment Service
    participant IS as Item Service
    participant DB as Cassandra
    
    F->>OS: POST /api/v1/orders/{id}/cancel
    activate OS
    
    OS->>DB: Update order<br/>status=CANCELED
    DB-->>OS: Updated
    
    OS->>IS: PATCH /api/v1/items<br/>Restore inventory
    IS-->>OS: Inventory restored
    
    OS->>K: Publish OrderCancelled<br/>{orderId}
    
    OS-->>F: 200 OK<br/>{status: CANCELED}
    deactivate OS
    
    Note over K: Async Event Processing
    
    K->>PS: Consume OrderCancelled
    activate PS
    
    PS->>DB: Update payment<br/>status=REFUNDED
    DB-->>PS: Updated
    
    deactivate PS
    
    Note over F,PS: Payment is now REFUNDED
```

## User Journey: Complete Purchase Flow

```mermaid
graph LR
    A[User Opens App] --> B[Login/Register]
    B --> C[Browse Items]
    C --> D[Create Order]
    D --> E{Order Created<br/>Status: CREATED}
    E --> F[Click Pay Now]
    F --> G[Enter Amount]
    G --> H[Submit Payment]
    H --> I{Payment Success?}
    I -->|Yes| J[Publish PaymentSucceeded]
    I -->|No| K[Payment Failed]
    J --> L[Order Marked COMPLETED]
    L --> M[Inventory Deducted]
    M --> N[View Order Details]
    N --> O{Cancel Order?}
    O -->|Yes| P[Order Canceled]
    P --> Q[Publish OrderCancelled]
    Q --> R[Payment Refunded]
    R --> S[Inventory Restored]
    O -->|No| T[Order Complete]
    K --> U[Try Again]
    
    style E fill:#FFA500
    style L fill:#90EE90
    style P fill:#FF6B6B
    style R fill:#87CEEB
```

## Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant AS as Account Service
    participant DB as MySQL
    
    U->>F: Enter credentials
    F->>AS: POST /api/v1/auth/login<br/>{email, password}
    activate AS
    
    AS->>DB: SELECT user WHERE email=?
    DB-->>AS: User record
    
    AS->>AS: Verify password hash
    AS->>AS: Generate JWT token<br/>Signed with SECRET_KEY
    
    AS-->>F: 200 OK<br/>{token: "eyJhbGc..."}
    deactivate AS
    
    F->>F: Store token in<br/>localStorage
    
    Note over U,F: All subsequent requests<br/>include token
    
    U->>F: Browse items
    F->>AS: GET /api/v1/items<br/>Authorization: Bearer <token>
    AS->>AS: Validate JWT<br/>Extract user email
    AS-->>F: Protected resource
```

## Service-to-Service Communication

```mermaid
graph TB
    subgraph "Synchronous (REST)"
        PS[Payment Service] -->|GET /orders/{id}| OS[Order Service]
        OS -->|GET /items| IS[Item Service]
        OS -->|PATCH /items| IS
        style PS fill:#FFE5B4
        style OS fill:#FFE5B4
        style IS fill:#FFE5B4
    end
    
    subgraph "Asynchronous (Kafka Events)"
        PS2[Payment Service] -.->|PaymentSucceeded| K[Kafka]
        K -.->|PaymentSucceeded| OS2[Order Service]
        OS2 -.->|OrderCancelled| K
        K -.->|OrderCancelled| PS2
        style PS2 fill:#B4E5FF
        style OS2 fill:#B4E5FF
        style K fill:#FF6B6B
    end
```

## Data Flow: Create Order

```mermaid
flowchart TD
    A[User Selects Items] --> B[Frontend Calls<br/>POST /api/v1/orders]
    B --> C{JWT Valid?}
    C -->|No| D[401 Unauthorized]
    C -->|Yes| E[Extract User Email]
    E --> F[Call Item Service<br/>Get Item Details]
    F --> G[Calculate Total Amount]
    G --> H[Create Order Record<br/>status=CREATED]
    H --> I[Save to Cassandra]
    I --> J[Return Order Response<br/>to Frontend]
    J --> K[Display Order Details]
    K --> L[Show Pay Now Button]
    
    style C fill:#FFE5B4
    style H fill:#90EE90
```

## Kafka Event Architecture

```mermaid
graph LR
    subgraph "Payment Service"
        PS_P[Producer]
        PS_C[Consumer]
    end
    
    subgraph "Kafka Cluster"
        T1[payment.events]
        T2[order.events]
    end
    
    subgraph "Order Service"
        OS_P[Producer]
        OS_C[Consumer]
    end
    
    PS_P -->|PaymentSucceeded| T1
    T1 -->|Consume| OS_C
    
    OS_P -->|OrderCancelled| T2
    T2 -->|Consume| PS_C
    
    style T1 fill:#FF6B6B
    style T2 fill:#FF6B6B
```

## State Machine: Order Lifecycle

```mermaid
stateDiagram-v2
    [*] --> CREATED: User creates order
    CREATED --> COMPLETED: PaymentSucceeded event
    CREATED --> CANCELED: User cancels unpaid order
    COMPLETED --> CANCELED: User cancels paid order
    CANCELED --> [*]: Final state
    
    note right of CREATED
        Order awaiting payment
        Can be edited
    end note
    
    note right of COMPLETED
        Payment received
        Inventory deducted
    end note
    
    note right of CANCELED
        Order canceled
        Inventory restored
        Payment refunded (if paid)
    end note
```

## State Machine: Payment Lifecycle

```mermaid
stateDiagram-v2
    [*] --> SUCCESS: Payment submitted successfully
    [*] --> FAILED: Payment validation failed
    SUCCESS --> REFUNDED: OrderCancelled event
    FAILED --> [*]: Terminal state
    REFUNDED --> [*]: Terminal state
    
    note right of SUCCESS
        Payment completed
        Order will be marked COMPLETED
    end note
    
    note right of FAILED
        Payment rejected
        Order remains CREATED
    end note
    
    note right of REFUNDED
        Payment refunded
        Due to order cancellation
    end note
```

## Technology Stack

```mermaid
graph TB
    subgraph "Presentation Layer"
        FE[React 18 + TypeScript + Vite]
    end
    
    subgraph "Application Layer"
        SB[Spring Boot 3.x + Java 21]
        SEC[Spring Security + JWT]
        VAL[Validation]
    end
    
    subgraph "Data Access Layer"
        JPA[Spring Data JPA]
        CAS[Spring Data Cassandra]
    end
    
    subgraph "Integration Layer"
        KAFKA[Spring Kafka]
        REST[RestTemplate]
    end
    
    subgraph "Infrastructure"
        DB1[(MySQL 8.0)]
        DB2[(Cassandra 4.1)]
        MSG[Kafka 3.9]
        DOC[Docker Compose]
    end
    
    FE --> SB
    SB --> SEC
    SB --> VAL
    SB --> JPA
    SB --> CAS
    SB --> KAFKA
    SB --> REST
    JPA --> DB1
    CAS --> DB2
    KAFKA --> MSG
    
    style FE fill:#61dafb
    style SB fill:#6DB33F
    style MSG fill:#FF6B6B
    style DOC fill:#2496ED
```

---

## How to Use These Diagrams

### 1. **GitHub Rendering**
- These Mermaid diagrams will automatically render on GitHub
- Just push the `.md` file and view it

### 2. **VS Code**
- Install "Markdown Preview Mermaid Support" extension
- Preview the file

### 3. **Export to Images**
- Use [Mermaid Live Editor](https://mermaid.live/)
- Copy/paste the diagram code
- Export as PNG/SVG

### 4. **PowerPoint/Presentations**
- Export diagrams as PNG
- Import into slides
- Or use "Mermaid for PowerPoint" add-in

### 5. **Documentation Sites**
- Most static site generators support Mermaid
- Docusaurus, VuePress, GitBook, etc.

---

**Created**: November 15, 2025  
**Format**: Mermaid Diagrams  
**Purpose**: Demo & Presentation

