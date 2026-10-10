import { createHmac, timingSafeEqual } from 'crypto';
import { Injectable, ServiceUnavailableException } from '@nestjs/common';

export interface GatewayOrder { id:string; amount:number; currency:string; }
@Injectable()
export class RazorpayProvider {
  private credentials(){const id=process.env.RAZORPAY_KEY_ID,secret=process.env.RAZORPAY_KEY_SECRET;if(!id||!secret)throw new ServiceUnavailableException('Online payments are not configured');return{id,secret};}
  publicKey(){return this.credentials().id;}
  async createOrder(amount:number,currency:string,receipt:string):Promise<GatewayOrder>{const {id,secret}=this.credentials();const response=await fetch('https://api.razorpay.com/v1/orders',{method:'POST',headers:{Authorization:`Basic ${Buffer.from(`${id}:${secret}`).toString('base64')}`,'Content-Type':'application/json'},body:JSON.stringify({amount,currency,receipt})});const body:any=await response.json();if(!response.ok||!body.id)throw new ServiceUnavailableException('Payment provider could not create an order');return{id:body.id,amount:body.amount,currency:body.currency};}
  verify(orderId:string,paymentId:string,signature:string){const {secret}=this.credentials();const expected=createHmac('sha256',secret).update(`${orderId}|${paymentId}`).digest();const actual=Buffer.from(signature,'hex');return actual.length===expected.length&&timingSafeEqual(actual,expected);}
  verifyWebhook(raw:Buffer,signature:string){const secret=process.env.RAZORPAY_WEBHOOK_SECRET;if(!secret)return false;const expected=createHmac('sha256',secret).update(raw).digest();const actual=Buffer.from(signature,'hex');return actual.length===expected.length&&timingSafeEqual(actual,expected);}
}
