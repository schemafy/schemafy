import { Controller, Get, Post, Headers } from '@nestjs/common';
import { AuthService } from './auth.service';

@Controller('/api/v1.0')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Get('/users')
  getMyInfo(@Headers('authorization') authHeader: string) {
    return this.authService.getMyInfo(authHeader);
  }

  @Post('/users/logout')
  logout(@Headers('authorization') authHeader: string) {
    return this.authService.logout(authHeader);
  }
}
