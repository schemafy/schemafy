package com.schemafy.domain.erd.vendor.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface DbVendorRepository extends ReactiveCrudRepository<DbVendorEntity, String> {
}
