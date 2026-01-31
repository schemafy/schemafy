package com.schemafy.domain.erd.index.fixture;

import java.util.List;

import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnResult;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexResult;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.domain.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;

public class IndexFixture {

  // Default Index constants
  public static final String DEFAULT_ID = "01ARZ3NDEKTSV4RRFFQ69G5IDX";
  public static final String DEFAULT_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5TBL";
  public static final String DEFAULT_NAME = "idx_test_index";
  public static final IndexType DEFAULT_TYPE = IndexType.BTREE;

  // Default IndexColumn constants
  public static final String DEFAULT_INDEX_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5ICL";
  public static final String DEFAULT_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5COL";
  public static final int DEFAULT_SEQ_NO = 0;
  public static final SortDirection DEFAULT_SORT_DIRECTION = SortDirection.ASC;

  // Other constants
  public static final String DEFAULT_SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";

  // ========== Index Domain Object Factory Methods ==========

  public static Index defaultIndex() {
    return new Index(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_TYPE);
  }

  public static Index indexWithId(String id) {
    return new Index(
        id,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_TYPE);
  }

  public static Index indexWithName(String name) {
    return new Index(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        name,
        DEFAULT_TYPE);
  }

  public static Index indexWithIdAndName(String id, String name) {
    return new Index(
        id,
        DEFAULT_TABLE_ID,
        name,
        DEFAULT_TYPE);
  }

  public static Index indexWithTableId(String tableId) {
    return new Index(
        DEFAULT_ID,
        tableId,
        DEFAULT_NAME,
        DEFAULT_TYPE);
  }

  public static Index indexWithType(IndexType type) {
    return new Index(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        type);
  }

  public static Index btreeIndex() {
    return new Index(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "idx_btree",
        IndexType.BTREE);
  }

  public static Index btreeIndexWithId(String id) {
    return new Index(
        id,
        DEFAULT_TABLE_ID,
        "idx_btree",
        IndexType.BTREE);
  }

  public static Index hashIndex() {
    return new Index(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "idx_hash",
        IndexType.HASH);
  }

  public static Index hashIndexWithId(String id) {
    return new Index(
        id,
        DEFAULT_TABLE_ID,
        "idx_hash",
        IndexType.HASH);
  }

  public static Index fulltextIndex() {
    return new Index(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "idx_fulltext",
        IndexType.FULLTEXT);
  }

  public static Index spatialIndex() {
    return new Index(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "idx_spatial",
        IndexType.SPATIAL);
  }

  public static Index index(String id, String tableId, String name, IndexType type) {
    return new Index(id, tableId, name, type);
  }

  // ========== IndexColumn Domain Object Factory Methods ==========

  public static IndexColumn defaultIndexColumn() {
    return new IndexColumn(
        DEFAULT_INDEX_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO,
        DEFAULT_SORT_DIRECTION);
  }

  public static IndexColumn indexColumnWithId(String id) {
    return new IndexColumn(
        id,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO,
        DEFAULT_SORT_DIRECTION);
  }

  public static IndexColumn indexColumnWithSeqNo(int seqNo) {
    return new IndexColumn(
        DEFAULT_INDEX_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        seqNo,
        DEFAULT_SORT_DIRECTION);
  }

  public static IndexColumn indexColumnWithIndexId(String indexId) {
    return new IndexColumn(
        DEFAULT_INDEX_COLUMN_ID,
        indexId,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO,
        DEFAULT_SORT_DIRECTION);
  }

  public static IndexColumn indexColumnWithColumnId(String columnId) {
    return new IndexColumn(
        DEFAULT_INDEX_COLUMN_ID,
        DEFAULT_ID,
        columnId,
        DEFAULT_SEQ_NO,
        DEFAULT_SORT_DIRECTION);
  }

  public static IndexColumn indexColumnWithSortDirection(SortDirection sortDirection) {
    return new IndexColumn(
        DEFAULT_INDEX_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO,
        sortDirection);
  }

  public static IndexColumn ascIndexColumn() {
    return new IndexColumn(
        DEFAULT_INDEX_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO,
        SortDirection.ASC);
  }

  public static IndexColumn descIndexColumn() {
    return new IndexColumn(
        DEFAULT_INDEX_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO,
        SortDirection.DESC);
  }

  public static IndexColumn indexColumn(
      String id, String indexId, String columnId, int seqNo, SortDirection sortDirection) {
    return new IndexColumn(id, indexId, columnId, seqNo, sortDirection);
  }

  // ========== CreateIndexCommand Factory Methods ==========

  public static CreateIndexCommand createCommand() {
    return new CreateIndexCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_TYPE,
        List.of(new CreateIndexColumnCommand(
            DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, DEFAULT_SORT_DIRECTION)));
  }

  public static CreateIndexCommand createCommandWithName(String name) {
    return new CreateIndexCommand(
        DEFAULT_TABLE_ID,
        name,
        DEFAULT_TYPE,
        List.of(new CreateIndexColumnCommand(
            DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, DEFAULT_SORT_DIRECTION)));
  }

  public static CreateIndexCommand createCommandWithTableId(String tableId) {
    return new CreateIndexCommand(
        tableId,
        DEFAULT_NAME,
        DEFAULT_TYPE,
        List.of(new CreateIndexColumnCommand(
            DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, DEFAULT_SORT_DIRECTION)));
  }

  public static CreateIndexCommand createCommandWithType(IndexType type) {
    return new CreateIndexCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        type,
        List.of(new CreateIndexColumnCommand(
            DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, DEFAULT_SORT_DIRECTION)));
  }

  public static CreateIndexCommand createBtreeCommand() {
    return new CreateIndexCommand(
        DEFAULT_TABLE_ID,
        "idx_btree",
        IndexType.BTREE,
        List.of(new CreateIndexColumnCommand(
            DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, SortDirection.ASC)));
  }

  public static CreateIndexCommand createBtreeCommandWithColumns(
      List<CreateIndexColumnCommand> columns) {
    return new CreateIndexCommand(
        DEFAULT_TABLE_ID,
        "idx_btree",
        IndexType.BTREE,
        columns);
  }

  public static CreateIndexCommand createHashCommand() {
    return new CreateIndexCommand(
        DEFAULT_TABLE_ID,
        "idx_hash",
        IndexType.HASH,
        List.of(new CreateIndexColumnCommand(
            DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, SortDirection.ASC)));
  }

  public static CreateIndexCommand createFulltextCommand() {
    return new CreateIndexCommand(
        DEFAULT_TABLE_ID,
        "idx_fulltext",
        IndexType.FULLTEXT,
        List.of(new CreateIndexColumnCommand(
            DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, SortDirection.ASC)));
  }

  public static CreateIndexCommand createSpatialCommand() {
    return new CreateIndexCommand(
        DEFAULT_TABLE_ID,
        "idx_spatial",
        IndexType.SPATIAL,
        List.of(new CreateIndexColumnCommand(
            DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, SortDirection.ASC)));
  }

  public static CreateIndexCommand createCommandWithColumns(
      List<CreateIndexColumnCommand> columns) {
    return new CreateIndexCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_TYPE,
        columns);
  }

  // ========== CreateIndexColumnCommand Factory Methods ==========

  public static CreateIndexColumnCommand createColumnCommand() {
    return new CreateIndexColumnCommand(
        DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, DEFAULT_SORT_DIRECTION);
  }

  public static CreateIndexColumnCommand createColumnCommand(
      String columnId, int seqNo, SortDirection sortDirection) {
    return new CreateIndexColumnCommand(columnId, seqNo, sortDirection);
  }

  public static CreateIndexColumnCommand createAscColumnCommand(String columnId, int seqNo) {
    return new CreateIndexColumnCommand(columnId, seqNo, SortDirection.ASC);
  }

  public static CreateIndexColumnCommand createDescColumnCommand(String columnId, int seqNo) {
    return new CreateIndexColumnCommand(columnId, seqNo, SortDirection.DESC);
  }

  // ========== Other Command Factory Methods ==========

  public static ChangeIndexNameCommand changeNameCommand(String newName) {
    return new ChangeIndexNameCommand(DEFAULT_ID, newName);
  }

  public static ChangeIndexNameCommand changeNameCommand(String indexId, String newName) {
    return new ChangeIndexNameCommand(indexId, newName);
  }

  public static ChangeIndexTypeCommand changeTypeCommand(IndexType type) {
    return new ChangeIndexTypeCommand(DEFAULT_ID, type);
  }

  public static ChangeIndexTypeCommand changeTypeCommand(String indexId, IndexType type) {
    return new ChangeIndexTypeCommand(indexId, type);
  }

  public static AddIndexColumnCommand addColumnCommand() {
    return new AddIndexColumnCommand(
        DEFAULT_ID, DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO, DEFAULT_SORT_DIRECTION);
  }

  public static AddIndexColumnCommand addColumnCommand(
      String indexId, String columnId, int seqNo, SortDirection sortDirection) {
    return new AddIndexColumnCommand(indexId, columnId, seqNo, sortDirection);
  }

  public static RemoveIndexColumnCommand removeColumnCommand() {
    return new RemoveIndexColumnCommand(DEFAULT_ID, DEFAULT_INDEX_COLUMN_ID);
  }

  public static RemoveIndexColumnCommand removeColumnCommand(
      String indexId, String indexColumnId) {
    return new RemoveIndexColumnCommand(indexId, indexColumnId);
  }

  public static ChangeIndexColumnPositionCommand changeColumnPositionCommand(int seqNo) {
    return new ChangeIndexColumnPositionCommand(DEFAULT_INDEX_COLUMN_ID, seqNo);
  }

  public static ChangeIndexColumnPositionCommand changeColumnPositionCommand(
      String indexColumnId, int seqNo) {
    return new ChangeIndexColumnPositionCommand(indexColumnId, seqNo);
  }

  public static ChangeIndexColumnSortDirectionCommand changeSortDirectionCommand(
      SortDirection sortDirection) {
    return new ChangeIndexColumnSortDirectionCommand(DEFAULT_INDEX_COLUMN_ID, sortDirection);
  }

  public static ChangeIndexColumnSortDirectionCommand changeSortDirectionCommand(
      String indexColumnId, SortDirection sortDirection) {
    return new ChangeIndexColumnSortDirectionCommand(indexColumnId, sortDirection);
  }

  public static DeleteIndexCommand deleteCommand() {
    return new DeleteIndexCommand(DEFAULT_ID);
  }

  public static DeleteIndexCommand deleteCommand(String indexId) {
    return new DeleteIndexCommand(indexId);
  }

  // ========== Query Factory Methods ==========

  public static GetIndexQuery getIndexQuery() { return new GetIndexQuery(DEFAULT_ID); }

  public static GetIndexQuery getIndexQuery(String indexId) {
    return new GetIndexQuery(indexId);
  }

  public static GetIndexesByTableIdQuery getIndexesByTableIdQuery() {
    return new GetIndexesByTableIdQuery(DEFAULT_TABLE_ID);
  }

  public static GetIndexesByTableIdQuery getIndexesByTableIdQuery(String tableId) {
    return new GetIndexesByTableIdQuery(tableId);
  }

  public static GetIndexColumnQuery getIndexColumnQuery() { return new GetIndexColumnQuery(DEFAULT_INDEX_COLUMN_ID); }

  public static GetIndexColumnQuery getIndexColumnQuery(String indexColumnId) {
    return new GetIndexColumnQuery(indexColumnId);
  }

  public static GetIndexColumnsByIndexIdQuery getIndexColumnsByIndexIdQuery() {
    return new GetIndexColumnsByIndexIdQuery(DEFAULT_ID);
  }

  public static GetIndexColumnsByIndexIdQuery getIndexColumnsByIndexIdQuery(String indexId) {
    return new GetIndexColumnsByIndexIdQuery(indexId);
  }

  // ========== Result Factory Methods ==========

  public static CreateIndexResult createResult() {
    return new CreateIndexResult(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_TYPE);
  }

  public static CreateIndexResult createResultFrom(Index index) {
    return new CreateIndexResult(
        index.id(),
        index.name(),
        index.type());
  }

  public static AddIndexColumnResult addColumnResult() {
    return new AddIndexColumnResult(
        DEFAULT_INDEX_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO,
        DEFAULT_SORT_DIRECTION);
  }

  public static AddIndexColumnResult addColumnResultFrom(IndexColumn indexColumn) {
    return new AddIndexColumnResult(
        indexColumn.id(),
        indexColumn.indexId(),
        indexColumn.columnId(),
        indexColumn.seqNo(),
        indexColumn.sortDirection());
  }

  private IndexFixture() {}

}
