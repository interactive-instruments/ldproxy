# Getting started

overview, dev setup 


# Workflow

## Contributing

## PR Reviews


# Composition

## Layers

collection of modules, gradle multi-module project

## Modules 

### Maturity

- **Production**
  - functionality: must implement whole spec or all currently required use cases
  - design: [rules](#design-rules) have to be followed
  - tests: 100% coverage
  - documentation: must be complete, all languages
  - backwards compatibility: might be broken in the next major release (cfg, java api, rest api ???)
  - lifetime: might be deprecated in the next major release and removed in the major release after that

- **Candidate**
  - functionality: should implement whole spec or reasonable set of use cases
  - design: [rules](#design-rules) should be followed, if not provide good reason
  - tests: at least 50% coverage
  - documentation: should be complete, maybe only certain languages
  - backwards compatibility: might be broken in the next major release (cfg, java api, rest api ???)
  - lifetime: might be deprecated in the next minor release and removed in the next major release

- **Experimental**
  - functionality: might implement only part of a spec or very specific use case
  - design: [rules](#design-rules) might be violated
  - tests: not required
  - documentation: minimum required to use it
  - backwards compatibility: might be broken in the next minor release (cfg, java api, rest api ???)
  - lifetime: might be removed in the next minor release

### Deprecated

## Components 

Implementation of one or more interfaces, typically a class.

### Maturity

Is there really a need for *Candidate* and *Experimental*? Otherwise `@Experimental` would be sufficient.

- **Production**

- **Candidate**

- **Experimental**

### Deprecated


# Design rules

## Modules

## Components
  - use functionality from existing module wherever possible, e.g. http client


# Reference

layers, modules and javadocs