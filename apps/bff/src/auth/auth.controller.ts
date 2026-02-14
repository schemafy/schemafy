import { Controller, Get, Post, Res } from '@nestjs/common';
import * as express from 'express';
import { AuthService } from './auth.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';

@Controller('/api/v1.0')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Get('/users')
  async getMyInfo(@AuthHeader() authHeader: string) {
    return this.authService.getMyInfo(authHeader);
  }

  @Post('/users/logout')
  async logout(
    @Res({ passthrough: true }) res: express.Response,
    @AuthHeader() authHeader: string,
  ) {
    const { data, setCookies } = await this.authService.logout(authHeader);

    for (const cookie of setCookies) {
      res.append('Set-Cookie', cookie);
    }

    return data;
  }
}
