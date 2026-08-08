package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.port.in.ProjectSearchResult;
import com.schemafy.core.project.application.port.in.SearchSharedProjectsQuery;
import com.schemafy.core.project.application.port.in.SearchSharedProjectsUseCase;
import com.schemafy.core.project.application.port.out.ProjectSearchPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class SearchSharedProjectsService implements SearchSharedProjectsUseCase {

  private final ProjectSearchPort projectSearchPort;

  @Override
  public Mono<PageResult<ProjectSearchResult>> searchSharedProjects(
      SearchSharedProjectsQuery query) {
    return projectSearchPort.searchSharedProjects(
        query.requesterId(), query.search(), query.page(), query.size());
  }

}
