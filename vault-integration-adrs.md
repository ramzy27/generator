# Vault Multi-Endpoint API Integration - Architecture Decision Records

## ADR001: Vault API Integration Approach

### Status
Proposed

### Context
Our Java 21 / Spring Boot 3 application needs to integrate with Vault's new multi-endpoint API, which now separates:
- Error position information (JSON format)
- Parquet file listings for a job
- Individual Parquet file download endpoints (returning application/octet-stream)

The application currently uses RestTemplate for API integrations, but we should consider adopting more modern approaches. This integration needs to handle potentially large files (up to 50MB per Parquet file) efficiently, with the data processing happening daily for a small number of jobs.

### Options Considered

#### Option 1: Continue Using RestTemplate
- Use existing RestTemplate configuration for Vault API integration
- Update to handle the new multi-endpoint structure
- **Pros:**
  - Familiar to the team
  - No learning curve
  - Consistent with existing codebase
- **Cons:**
  - Being deprecated in favor of WebClient
  - Limited support for efficient streaming
  - Blocking nature can impact resource utilization

#### Option 2: Migrate to WebClient
- Use Spring WebClient to consume the Vault API endpoints
- Leverage its streaming capabilities for large octet-stream downloads
- **Pros:**
  - Modern replacement for RestTemplate
  - Superior streaming support for binary data
  - Non-blocking capabilities
  - Better memory management for large responses
- **Cons:**
  - Migration effort from RestTemplate
  - Learning curve for team members unfamiliar with reactive programming
  - Potential complexity in error handling

#### Option 3: Use Feign Client with Resilience4j
- Implement Vault API integration using OpenFeign client
- Add Resilience4j for circuit breaking and retries
- **Pros:**
  - Declarative REST client with minimal boilerplate
  - Built-in resilience patterns
  - Clean interface-based approach
- **Cons:**
  - Not well-suited for large binary file downloads
  - Limited streaming capabilities
  - Additional dependencies

### Decision
Selected **Option 2: Migrate to WebClient** because:
- Provides superior support for streaming binary data (octet-stream) efficiently
- Offers better memory management through non-blocking I/O
- Represents the strategic direction for Spring's HTTP client support
- Enables efficient handling of both JSON and binary data streams
- Allows for controlled backpressure when processing large files

### Consequences
- Gradual migration from RestTemplate to WebClient will be needed
- Team members will need to become familiar with WebClient API
- Will need to implement proper resource cleanup in WebClient usage
- More efficient memory utilization and better handling of large payloads

### Architecture Diagram

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "Application" {
  [WebClient] as WC
  [Error Position Processor] as EPP
  [Parquet Processor] as PP
  [Data Merger] as DM
}

cloud "Vault API" {
  [Error Positions Endpoint] as EPE
  [Parquet Listing Endpoint] as PLE
  [Parquet Download Endpoint] as PDE
}

WC --> EPE : GET JSON
WC --> PLE : GET JSON
WC --> PDE : GET octet-stream

EPE ..> EPP : Process
PDE ..> PP : Process
EPP --> DM : Merge
PP --> DM : Merge

@enduml
```

---

## ADR002: Parquet File Processing Strategy

### Status
Proposed

### Context
The application needs to download and process Parquet files that are delivered as octet-stream data from the Vault API. Each file can be up to 50MB. We need an efficient approach to:
- Download these potentially large binary streams
- Convert octet-stream to Parquet format for processing
- Parse the Parquet format
- Merge with error position data from a separate endpoint
- Process everything in a memory-efficient way
- Ensure complete memory cleanup after processing

### Options Considered

#### Option 1: Full In-Memory Byte Array Processing
- Download entire octet-stream into byte arrays in memory
- Convert to ByteArrayInputStream for Parquet processing
- Process using standard Apache Parquet libraries
- **Pros:**
  - Simple implementation
  - Direct processing path
  - Common approach for Parquet processing
- **Cons:**
  - High memory consumption for multiple files
  - Potential OutOfMemoryErrors under load
  - Complete file must be in memory before processing starts

#### Option 2: Streaming Buffer Processing
- Stream octet-stream data through fixed-size buffers
- Use InputStreams with Apache Parquet libraries
- Process data as it arrives rather than waiting for complete download
- **Pros:**
  - More memory efficient
  - Processing can begin before download completes
  - Controlled buffer sizes prevent memory spikes
- **Cons:**
  - More complex implementation
  - Requires careful stream management
  - Need to handle streaming errors properly

#### Option 3: Memory-Mapped Buffer Approach
- Download to temporary ByteBuffers
- Use memory-mapped approach for efficient reading
- **Pros:**
  - Efficient memory usage for large files
  - Good performance characteristics
  - Works well with Java NIO
- **Cons:**
  - More complex implementation
  - Potential native memory issues if not managed properly
  - Requires more careful resource cleanup

### Decision
Selected **Option 2: Streaming Buffer Processing** because:
- Provides memory-efficient processing of large octet-stream data
- Enables processing to start before complete file is downloaded
- Works well with WebClient's streaming capabilities
- Prevents large memory spikes when processing multiple files
- Better supports the atomic processing requirement with controlled resource usage

### Consequences
- Need to implement proper stream handling and buffer management
- Must ensure all streams are properly closed after processing
- More controlled memory usage patterns
- Slightly more complex implementation but better scalability

### Architecture Diagram

```plantuml
@startuml
!theme plain
skinparam sequenceMessageAlign center

participant "WebClient" as WC
participant "Buffer Manager" as BM
participant "Parquet Reader" as PR
participant "Data Processor" as DP
participant "Resource Cleaner" as RC

WC -> BM : Stream octet-stream data
activate BM

loop Until end of stream
  BM -> PR : Process buffer chunk
  activate PR
  PR -> DP : Process Parquet data
  activate DP
  DP --> PR : Return processed result
  deactivate DP
  PR --> BM : Buffer processed
  deactivate PR
end

BM -> RC : Signal completion
activate RC
RC -> BM : Release buffers
RC -> PR : Close readers
RC -> DP : Clean resources
deactivate RC
deactivate BM

@enduml
```

---

## ADR003: Memory Management Strategy

### Status
Proposed

### Context
Processing large Parquet files (up to 50MB each) delivered as octet-stream requires careful attention to memory management. We need to ensure that resources are properly released after processing, treating each job as an atomic operation that completely cleans up after completion.

### Options Considered

#### Option 1: Rely on Garbage Collection
- Use standard Java objects and references
- Let JVM garbage collection handle memory cleanup
- **Pros:**
  - Simplest implementation
  - Standard Java approach
  - No custom cleanup code
- **Cons:**
  - Unpredictable memory release timing
  - May lead to memory pressure during peak load
  - No explicit control over resource cleanup

#### Option 2: Manual Resource Management
- Explicitly close and dispose all streams and resources
- Use try-with-resources for all closeable objects
- Nullify references when processing completes
- **Pros:**
  - Deterministic resource cleanup
  - Better control over memory usage
  - More predictable memory footprint
- **Cons:**
  - Requires careful implementation
  - Risk of resource leaks if any cleanup is missed
  - More verbose code

#### Option 3: Resource Cleanup Framework
- Implement a custom resource tracking framework
- Register all resources created during a job
- Automatically clean up all registered resources at job completion
- **Pros:**
  - Centralized resource management
  - Lower risk of missed cleanups
  - Clear tracking of all allocated resources
- **Cons:**
  - Additional framework to maintain
  - Overhead of resource registration
  - More complex implementation

### Decision
Selected **Option 2: Manual Resource Management** because:
- Provides deterministic cleanup of resources after job processing
- Ensures each job processing is truly atomic with respect to memory usage
- Takes advantage of Java 21's enhanced try-with-resources and AutoCloseable pattern
- Simpler to implement correctly than a custom framework
- Direct control over when resources are released

### Consequences
- Need to implement consistent cleanup patterns throughout the code
- Will require careful code reviews to ensure all resources are properly closed
- More predictable memory usage patterns
- Reduced risk of memory leaks or excessive memory consumption

### Architecture Diagram

```plantuml
@startuml
!theme plain
skinparam activityDiamondBackgroundColor white

start
:Create ResourceRegistry for job;

fork
  :Fetch error positions;
  :Process error positions;
fork again
  :Get Parquet file list;
  
  while (more files?) is (yes)
    :Open stream from API;
    :Process Parquet data;
    :Close stream;
  endwhile (no)
end fork

:Merge results;
:Process final data;

|Cleanup|
:Close all registered resources;
:Release all buffers;
:Clear all references;
stop

@enduml
```

---

## ADR004: Error Position Correlation Strategy

### Status
Proposed

### Context
The application needs to correlate error positions (received from one endpoint in JSON format) with the actual data in Parquet files (from separate endpoints). This correlation must be efficient and accurate for daily processing jobs, while maintaining our atomic processing approach.

### Options Considered

#### Option 1: Sequential Processing
- Download error positions first
- Then download and process each Parquet file sequentially
- Join error data with Parquet data during processing
- **Pros:**
  - Simple implementation
  - Minimal concurrency complexity
  - Predictable resource usage
  - Easier to ensure complete cleanup
- **Cons:**
  - Slower overall processing time
  - Inefficient for multiple files
  - Under-utilizes modern multi-core processors

#### Option 2: Parallel Processing with CompletableFuture
- Download error positions and Parquet file list concurrently
- Process Parquet files in parallel using CompletableFuture
- Join results when all processing completes
- **Pros:**
  - Better performance through parallelism
  - Efficient use of system resources
  - Built-in Java API
  - Appropriate for daily batch processing
- **Cons:**
  - More complex error handling
  - Careful thread pool management required
  - Resource cleanup must account for all parallel tasks

#### Option 3: Structured Concurrency (Java 21)
- Leverage Java 21's structured concurrency features
- Use virtual threads to handle parallel processing
- Ensure all threads complete before resources are released
- **Pros:**
  - Modern approach using Java 21 features
  - Automatic child thread management
  - Simplified error handling across threads
  - Natural fit for atomic operations
- **Cons:**
  - New API with less community experience
  - Different programming model
  - May require team training

### Decision
Selected **Option 3: Structured Concurrency (Java 21)** because:
- Takes advantage of the latest Java 21 capabilities
- Structured concurrency model aligns perfectly with atomic operation requirements
- Provides a clean approach to parallel processing with proper resource scoping
- Ensures all subtasks complete (successfully or with errors) before releasing resources
- More efficient than sequential processing while maintaining clean resource management

### Consequences
- Team will need to learn structured concurrency patterns
- Need to carefully implement scoped values for resource sharing
- Will benefit from improved error handling across concurrent operations
- Processing performance will scale better with multiple files
- Resource cleanup will be more deterministic after job completion

### Architecture Diagram

```plantuml
@startuml
!theme plain
skinparam sequenceMessageAlign center

participant "Main Thread" as MT
participant "Structured Scope" as SS
participant "Error Positions Task" as EPT
participant "Parquet List Task" as PLT
participant "Parquet Processor Tasks" as PPT
participant "Result Merger" as RM

MT -> SS : Create structured scope
activate SS

SS -> EPT : Fork error positions task
activate EPT
SS -> PLT : Fork Parquet list task
activate PLT

PLT -> PPT : Create tasks for each Parquet file
activate PPT

EPT --> SS : Complete with error positions
deactivate EPT
PPT --> SS : Complete with processed Parquet data
deactivate PPT
PLT --> SS : Complete with file processing results
deactivate PLT

SS -> RM : Join all results
activate RM
RM --> SS : Return final merged data
deactivate RM

SS --> MT : Return final result
deactivate SS

MT -> MT : Clean up all resources

@enduml
```

# ADR010: Parquet File Processing Strategy

## Status
Proposed

## Context
Our system needs to process Parquet files efficiently, with specific requirements:
- Handle files up to 50MB in size
- Perform memory-conscious processing
- Convert Parquet data to domain objects
- Support multiple file consolidation for a single job

## Options Considered

### Option 1: Apache Parquet Native Java Library with Iterative Processing
**Pros:**
- Native support for Parquet files
- Lightweight implementation
- Direct integration with Java ecosystem
- Minimal external dependencies
- Straightforward parsing mechanism

**Cons:**
- Potential performance overhead for very large files
- Manual memory management required
- Less advanced parsing capabilities compared to specialized libraries

### Option 2: Apache Spark for Parquet Processing
**Pros:**
- Robust big data processing
- Advanced parsing capabilities
- Distributed computing support

**Cons:**
- Heavyweight solution
- Significant overhead for small to medium files
- Complex setup and configuration
- Overkill for our current use case

### Option 3: Custom Streaming Parquet Processor
**Pros:**
- Complete control over processing
- Optimized for specific use case
- Minimal dependencies

**Cons:**
- Increased development complexity
- Potential performance and reliability challenges
- Requires significant initial implementation effort

## Decision
**Selected Option: Option 1 - Apache Parquet Native Java Library with Iterative Processing**

Rationale:
- Simplest and most direct approach
- Aligns with project's complexity requirements
- Provides sufficient performance for expected file sizes
- Minimizes external dependencies
- Easy to understand and maintain

## Consequences
**Positive:**
- Simple implementation
- Low computational overhead
- Easy to test and debug
- Minimal external library dependencies

**Negative:**
- Manual implementation of parsing logic
- Potential performance limitations for extremely large files
- Requires careful memory management

**Technical Debt:**
- Need for comprehensive error handling
- Potential future optimization requirements
- Ongoing performance monitoring

## Processing Strategy
1. Retrieve list of Parquet file URLs for a job
2. Download files sequentially
3. Parse each file using Apache Parquet library
4. Accumulate records into a consolidated collection
5. Apply error position matching after full processing

## Memory Management Considerations
- Process files iteratively
- Use streaming techniques to minimize memory consumption
- Implement incremental record collection
- Consider pagination or chunked processing for very large datasets

## Recommended Implementation Details
1. Use `ParquetReader` for file parsing
2. Implement incremental record collection
3. Create utility classes for Parquet-to-domain object mapping
4. Develop comprehensive error handling mechanisms
5. Design flexible record aggregation strategy

## Recommended Next Steps
1. Select appropriate Apache Parquet Java library
2. Create proof-of-concept implementation
3. Develop comprehensive test scenarios
4. Benchmark processing performance
5. Document processing strategy and considerations
