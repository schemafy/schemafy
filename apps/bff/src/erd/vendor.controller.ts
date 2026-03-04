import { Controller, Get, Param } from '@nestjs/common';
import { VendorService } from './vendor.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';

@Controller('api/v1.0')
export class VendorController {
  constructor(private readonly vendorService: VendorService) {}

  @Get('vendors')
  async listVendors(@AuthHeader() authHeader: string) {
    return this.vendorService.listVendors(authHeader);
  }

  @Get('vendors/:displayName')
  async getVendor(
    @Param('displayName') displayName: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.vendorService.getVendor(displayName, authHeader);
  }
}
