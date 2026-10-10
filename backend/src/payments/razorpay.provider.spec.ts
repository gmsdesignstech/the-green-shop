import { createHmac } from 'crypto';
import { RazorpayProvider } from './razorpay.provider';

describe('RazorpayProvider verification',()=>{
  beforeEach(()=>{process.env.RAZORPAY_KEY_ID='rzp_test_public';process.env.RAZORPAY_KEY_SECRET='test_secret';process.env.RAZORPAY_WEBHOOK_SECRET='webhook_secret';});
  it('accepts a valid checkout signature',()=>{const signature=createHmac('sha256','test_secret').update('order_abc|pay_xyz').digest('hex');expect(new RazorpayProvider().verify('order_abc','pay_xyz',signature)).toBe(true);});
  it('rejects an invalid checkout signature',()=>expect(new RazorpayProvider().verify('order_abc','pay_xyz','00'.repeat(32))).toBe(false));
  it('verifies the unmodified webhook body',()=>{const raw=Buffer.from('{"event":"payment.captured"}');const signature=createHmac('sha256','webhook_secret').update(raw).digest('hex');const provider=new RazorpayProvider();expect(provider.verifyWebhook(raw,signature)).toBe(true);expect(provider.verifyWebhook(Buffer.from(raw+' '),signature)).toBe(false);});
});
