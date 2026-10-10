import { PaymentsService } from './payments.service';

describe('PaymentsService',()=>{
  it('reuses an existing attempt for an idempotency key',async()=>{const db={query:jest.fn().mockResolvedValue({rowCount:1,rows:[{id:'a',order_id:9,provider:'razorpay',status:'pending',amount_minor:'12000',currency:'INR',gateway_order_id:'order_x'}]}),transaction:jest.fn()} as any;const service=new PaymentsService(db,{} as any);const result=await service.checkout('user',{items:[{productId:1,quantity:1}],method:'razorpay',idempotencyKey:'checkout-123'});expect(result).toEqual(expect.objectContaining({attemptId:'a',orderId:9,amountMinor:12000}));expect(db.transaction).not.toHaveBeenCalled();});
});
