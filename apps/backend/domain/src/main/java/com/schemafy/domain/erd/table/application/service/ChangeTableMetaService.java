package com.schemafy.domain.erd.table.application.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableMetaUseCase;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableMetaPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeTableMetaService implements ChangeTableMetaUseCase {

	private final ChangeTableMetaPort changeTableMetaPort;

	@Override
	public Mono<MutationResult<Void>> changeTableMeta(ChangeTableMetaCommand command) {
		String portCharset = command.charset().isPresent()
				? normalizeForPort(command.charset().get())
				: null;
		String portCollation = command.collation().isPresent()
				? normalizeForPort(command.collation().get())
				: null;

		return changeTableMetaPort.changeTableMeta(
				command.tableId(),
				portCharset,
				portCollation)
				.thenReturn(MutationResult.<Void>of(null, command.tableId()));
	}

	private static String normalizeForPort(String value) {
		return Objects.toString(normalizeOptional(value), "");
	}

	private static String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

}
