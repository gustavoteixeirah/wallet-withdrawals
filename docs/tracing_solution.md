# Tracing Solution for Wallet Withdrawal SAGA

## 1. Overview & Architecture

### Goal
To implement an end-to-end distributed tracing solution for the SAGA-based withdrawal process, ensuring trace context is propagated from the initial API request, through asynchronous event listeners, and across all external HTTP calls.

### Core Technologies
The solution leverages Spring Boot 3, OpenTelemetry (OTel), AOP, and the Decorator pattern.

### Architectural Rationale

#### Decoupling
The solution is intentionally isolated in the infrastructure layer, leaving the application and domain layers 100% clean of any tracing-specific code.

#### Automation
Tracing for use cases and event listeners is automatic, achieved by targeting existing annotations (@TransactionalUseCase, @TransactionalEventListener). This shows you value developer experience and reducing boilerplate.

#### Explicitness
We used a Decorator (TracingDomainEventPublisherDecorator) for the event publisher. This shows you understand the pattern: it's a cleaner way to add functionality (injecting trace headers) without modifying the original class's logic.

#### Leveraging Frameworks
For HTTP clients, the solution uses RestTemplateBuilder to automatically hook into Spring Boot's OTel auto-configuration. This shows you know when not to reinvent the wheel.

## 2. How to Run & View

### Prerequisites
- Java 21
- Docker (for the OTel Collector, database, etc.)

### Running the Stack
Launch all supporting services with: `docker-compose up -d`.  
This starts the database (PostgreSQL), OpenTelemetry Collector, Tempo (for traces), Loki (for logs), Prometheus (for metrics), and Grafana (for visualization).  
Traces can be viewed at the Tempo UI: http://localhost:3110 or through Grafana at http://localhost:3000 (admin/admin).

### Running the Application
./gradlew bootRun

### Triggering a Trace
Trigger the entire SAGA with a POST request to `/api/v1/wallet_withdraw` using the following sample JSON body:  
```json
{
    "userId": 1,
    "amount": 100.00,
    "recipientFirstName": "John",
    "recipientLastName": "Doe",
    "recipientRoutingNumber": "123456789",
    "recipientNationalId": "123456789",
    "recipientAccountNumber": "987654321"
}
```  
This will create a single trace in Tempo. You will see the parent span `InitiateWithdrawalUseCase`, followed by child spans like `SagaStep:DebitWallet` and `SagaStep:ProcessPayment` on different threads.

## 3. Developer's Guide (How to Extend)

### How to Trace a New Use Case
"No action needed. Just annotate the InputPort class with @TransactionalUseCase. Tracing is applied automatically by the UseCaseTracingAspect."

### How to Trace a New Event Listener
"No action needed. Just annotate your listener method with @TransactionalEventListener. The EventConsumerTracingAspect automatically picks it up, extracts the trace context from the event, and creates a new consumer span."

### How to Trace a New HTTP Client
"Create a new RestTemplate bean in HttpAdaptersConfiguration.java using the injected RestTemplateBuilder."
"Give it a unique @Bean("myNewClientRestTemplate") name."
"In your new adapter, inject it using @Qualifier("myNewClientRestTemplate")."

### How to Add Custom Span Names
- Use Cases: "Add the spanName attribute to the annotation (e.g., @TransactionalUseCase(spanName = "MyUseCase"))."
- Listeners: "Add the @NamedSpan("MySpanName") annotation to the listener method."
- HTTP/Other: "Use the @WithSpan("MySpanName") annotation directly on the adapter method."

### How to Add Custom Data (Attributes)
- Simple Data: "Annotate method parameters with @SpanAttribute("my.attribute.name") on any method that has a @WithSpan annotation."
- Complex Data: "Manually get the current span by calling Span.current() and use span.setAttribute("key", "value") to add custom attributes. Note: Be careful to scrub sensitive PII from payloads before adding them to spans."
