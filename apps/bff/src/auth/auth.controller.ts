import { Controller, Get, Post, Headers, Res } from '@nestjs/common';
import * as express from 'express';
import { AuthService } from './auth.service';

@Controller('/api/v1.0')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Get('/users')
  async getMyInfo(@Headers('authorization') authHeader: string) {
    return this.authService.getMyInfo(authHeader);
  }

  @Post('/users/logout')
  async logout(
    @Headers('authorization') authHeader: string,
    @Res({ passthrough: true }) res: express.Response,
  ) {
    const { data, setCookies } = await this.authService.logout(authHeader);

    for (const cookie of setCookies) {
      res.append('Set-Cookie', cookie);
    }

    return data;
  }
}
