package com.schemafy.api.erd.service.memo;

import org.springframework.stereotype.Component;

import com.schemafy.api.common.security.principal.AuthenticatedUser;
import com.schemafy.api.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.api.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.api.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.api.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.memo.application.port.in.CreateMemoCommand;
import com.schemafy.core.erd.memo.application.port.in.CreateMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommand;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.in.GetMemoCommentsQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemoQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemosBySchemaIdQuery;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoPositionCommand;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemoApiCommandMapper {

  private final MemoDeletePermissionPolicy permissionPolicy;
  private final JsonCodec jsonCodec;

  public CreateMemoCommand toCreateMemoCommand(
      CreateMemoRequest request,
      AuthenticatedUser user) {
    return new CreateMemoCommand(
        request.schemaId(),
        jsonCodec.canonicalize(request.positions()),
        request.body(),
        user.userId());
  }

  public GetMemoQuery toGetMemoQuery(String memoId) {
    return new GetMemoQuery(memoId);
  }

  public GetMemosBySchemaIdQuery toGetMemosBySchemaIdQuery(String schemaId) {
    return new GetMemosBySchemaIdQuery(schemaId);
  }

  public UpdateMemoPositionCommand toUpdateMemoPositionCommand(
      String memoId,
      UpdateMemoRequest request,
      AuthenticatedUser user) {
    return new UpdateMemoPositionCommand(
        memoId,
        jsonCodec.canonicalize(request.positions()),
        user.userId());
  }

  public DeleteMemoCommand toDeleteMemoCommand(
      String memoId,
      AuthenticatedUser user) {
    return new DeleteMemoCommand(
        memoId,
        user.userId(),
        permissionPolicy.canDeleteOthers(user));
  }

  public CreateMemoCommentCommand toCreateMemoCommentCommand(
      String memoId,
      CreateMemoCommentRequest request,
      AuthenticatedUser user) {
    return new CreateMemoCommentCommand(
        memoId,
        request.body(),
        user.userId());
  }

  public GetMemoCommentsQuery toGetMemoCommentsQuery(String memoId) {
    return new GetMemoCommentsQuery(memoId);
  }

  public UpdateMemoCommentCommand toUpdateMemoCommentCommand(
      String commentId,
      UpdateMemoCommentRequest request,
      AuthenticatedUser user) {
    return new UpdateMemoCommentCommand(
        commentId,
        request.body(),
        user.userId());
  }

  public DeleteMemoCommentCommand toDeleteMemoCommentCommand(
      String commentId,
      AuthenticatedUser user) {
    return new DeleteMemoCommentCommand(
        commentId,
        user.userId(),
        permissionPolicy.canDeleteOthers(user));
  }

}
