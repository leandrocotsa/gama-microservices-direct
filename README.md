# Microservices-based with syncnhronous communication e-commerce platform

This is the source code for a test application built for the purpose of my master's dissertation. 
This is one out of 3 different versions of the same application built for research purposes. This one features a microservices-based application with 
direct synchronous inter-service communication, other is also microservices-based but uses a message-driven approach ([link](https://github.com/leandrocosta16/gama-microservices)), 
and the last one follows a monolithic approach ([link](https://github.com/leandrocosta16/gama-monolith)).


## Stacks

A Docker Swarm cluster is used and it is composed of 4 stacks:
- Main application stack, composed of:
  - Inventory Serivce
  - Order Service 
  - Payment Service
  - Product Service
  - Payment Service
  - Promotion Service
  - Review Service
  - Shopping-cart Service
  - User Service
  - API Gateway
  - MySQL database
  
The main business services were built using Java and Spring Boot version `2.4.4`.

- Monitoring stack
  - Grafana
  - cAdvisor
  - Prometheus
  - Node exporter
  
- Logging & Tracing stack
  - Elastic Search
  - Logstash
  - Kibana
  - Zipkin

 
## Architecture

The next image represents an overview of the architecture of the main application stack, with the main business services.

![archietcture overview](https://raw.githubusercontent.com/leandrocosta16/gama-microservices-direct/main/imgs/rasp-performance-direct.png)
