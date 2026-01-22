import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Param,
  Post,
  Put,
} from '@nestjs/common';
import { MemoService } from './memo.service';
import type {
  CreateMemoCommentRequest,
  CreateMemoRequest,
  UpdateMemoCommentRequest,
  UpdateMemoRequest,
} from './memo.types';

@Controller('api/v1.0')
export class MemoController {
  constructor(private readonly memoService: MemoService) {}

  @Post('memos')
  async createMemo(
    @Body() data: CreateMemoRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.createMemo(data, authHeader);
  }

  @Get('memos/:memoId')
  async getMemo(
    @Param('memoId') memoId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.getMemo(memoId, authHeader);
  }

  @Get('schemas/:schemaId/memos')
  async getSchemaMemos(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.getSchemaMemos(schemaId, authHeader);
  }

  @Get('schemas/:schemaId/memos-with-comments')
  async getSchemaMemosWithComments(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.getSchemaMemosWithComments(schemaId, authHeader);
  }

  @Put('memos/:memoId')
  async updateMemo(
    @Param('memoId') memoId: string,
    @Body() data: UpdateMemoRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.updateMemo(memoId, data, authHeader);
  }

  @Delete('memos/:memoId')
  async deleteMemo(
    @Param('memoId') memoId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.deleteMemo(memoId, authHeader);
  }

  @Post('memos/:memoId/comments')
  async createMemoComment(
    @Param('memoId') memoId: string,
    @Body() data: CreateMemoCommentRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.createMemoComment(memoId, data, authHeader);
  }

  @Get('memos/:memoId/comments')
  async getMemoComments(
    @Param('memoId') memoId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.getMemoComments(memoId, authHeader);
  }

  @Put('memos/:memoId/comments/:commentId')
  async updateMemoComment(
    @Param('memoId') memoId: string,
    @Param('commentId') commentId: string,
    @Body() data: UpdateMemoCommentRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.updateMemoComment(
      memoId,
      commentId,
      data,
      authHeader,
    );
  }

  @Delete('memos/:memoId/comments/:commentId')
  async deleteMemoComment(
    @Param('memoId') memoId: string,
    @Param('commentId') commentId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.memoService.deleteMemoComment(memoId, commentId, authHeader);
  }
}
