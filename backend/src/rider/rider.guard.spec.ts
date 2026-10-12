import { ExecutionContext, ForbiddenException } from '@nestjs/common';
import { RiderGuard } from './rider.guard';

describe('RiderGuard',()=>{
 const context=(role?:'customer'|'admin'|'delivery_partner')=>({switchToHttp:()=>({getRequest:()=>({user:{id:'partner-1',role}})})}) as unknown as ExecutionContext;
 it('allows only an approved active trusted partner',async()=>{const db={query:vi.fn().mockResolvedValue({rowCount:1})} as any;await expect(new RiderGuard(db).canActivate(context())).resolves.toBe(true);expect(db.query).toHaveBeenCalledWith(expect.stringContaining("a.status='approved'"),['partner-1']);});
 it('rejects pending, rejected or suspended users',async()=>{const db={query:vi.fn().mockResolvedValue({rowCount:0})} as any;await expect(new RiderGuard(db).canActivate(context())).rejects.toBeInstanceOf(ForbiddenException);});
 it('accepts only the isolated development delivery role',async()=>{const db={query:vi.fn()} as any;await expect(new RiderGuard(db).canActivate(context('delivery_partner'))).resolves.toBe(true);await expect(new RiderGuard(db).canActivate(context('customer'))).rejects.toBeInstanceOf(ForbiddenException);await expect(new RiderGuard(db).canActivate(context('admin'))).rejects.toBeInstanceOf(ForbiddenException);expect(db.query).not.toHaveBeenCalled();});
});
