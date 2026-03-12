package com.schemafy.core.erd.vendor.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface DbVendorRepository extends ReactiveCrudRepository<DbVendorEntity, String> {
}
