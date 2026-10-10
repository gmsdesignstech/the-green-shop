package com.example.data.rider

import com.example.BuildConfig
import com.example.location.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

data class RiderProfile(val name:String,val serviceArea:String,val vehicleType:String,val online:Boolean,val availabilityVersion:Int)
data class RiderSummary(val todayCompleted:Int,val todayEarnings:String,val pendingEarnings:String,val pendingPayout:String)
data class RiderAssignment(val id:Long,val orderId:Long,val status:String,val version:Int,val pickupName:String,val destination:String)
data class RiderRow(val id:String,val title:String,val subtitle:String,val status:String)
data class RiderDashboard(val profile:RiderProfile,val summary:RiderSummary,val active:RiderAssignment?,val recent:List<RiderRow>,val notifications:List<RiderRow>)
class RiderApiException(val statusCode:Int,message:String):IllegalStateException(message)

class RiderRepository(private val client:OkHttpClient=OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()){
 private val baseUrl=BuildConfig.API_BASE_URL.trim().trimEnd('/')
 val isConfigured=(baseUrl.startsWith("https://")||(BuildConfig.DEBUG&&baseUrl.startsWith("http://")))&&!baseUrl.contains("YOUR_API_HOST")
 suspend fun dashboard(token:String)=request("rider/me",token,"GET") { j->
  val p=j.getJSONObject("profile");val s=j.getJSONObject("summary");val active=j.optJSONObject("activeAssignment")
  RiderDashboard(RiderProfile(p.optString("display_name"),p.optString("service_area"),p.optString("vehicle_type"),p.optBoolean("is_online"),p.optInt("availability_version")),
   RiderSummary(s.optInt("today_completed"),s.optString("today_earnings","0"),s.optString("pending_earnings","0"),s.optString("pending_payout","0")),
   active?.let(::assignment),j.optJSONArray("recentDeliveries").rows{RiderRow(it.optString("id"),"Order #${it.optString("order_id")}",it.optString("assigned_at"),it.optString("status"))},
   j.optJSONArray("notifications").rows{RiderRow(it.optString("id"),it.optString("title"),it.optString("message"),if(it.isNull("read_at"))"UNREAD" else "READ")})
 }
 suspend fun availability(token:String,online:Boolean,version:Int)=request("rider/me/availability",token,"PATCH",JSONObject().put("isOnline",online).put("version",version)){it}
 suspend fun action(token:String,a:RiderAssignment,target:String,otp:String?=null)=request("rider/deliveries/${a.id}/${target.replace('_','-')}",token,"POST",JSONObject().put("version",a.version).put("idempotencyKey",UUID.randomUUID().toString()).apply{if(!otp.isNullOrBlank())put("otp",otp)}){it}
 suspend fun sendLocation(token:String,assignmentId:Long,point:LocationData)=request("rider/deliveries/$assignmentId/locations",token,"POST",JSONObject().put("points",JSONArray().put(JSONObject().put("latitude",point.latitude).put("longitude",point.longitude).put("accuracyMeters",point.accuracy).put("recordedAt",point.timestamp.toUtcIso8601()).apply{point.bearing?.let{put("headingDegrees",it)};point.speed?.let{put("speedMps",it)}}))){it}
 suspend fun list(token:String,section:String):Result<List<RiderRow>>{val path=when(section){"History"->"rider/deliveries";"Earnings"->"rider/earnings";"Payouts"->"rider/payouts";"Notifications"->"rider/notifications";else->return Result.success(emptyList())};return request(path,token,"GET"){j->j.optJSONArray("items").rows{item->when(section){"History"->RiderRow(item.optString("id"),"Order #${item.optString("order_id")}","₹${item.optString("earnings","0")}",item.optString("status"));"Earnings"->RiderRow(item.optString("id"),"Delivery #${item.optString("assignment_id")}","₹${item.optString("base_amount")} + ₹${item.optString("incentive_amount")} incentive",item.optString("status"));"Payouts"->RiderRow(item.optString("id"),"₹${item.optString("amount")}",item.optString("destination_mask","Payout destination protected"),item.optString("status"));else->RiderRow(item.optString("id"),item.optString("title"),item.optString("message"),if(item.isNull("read_at"))"UNREAD" else "READ")}}}}
 suspend fun support(token:String,assignmentId:Long?,category:String,description:String)=request("rider/support",token,"POST",JSONObject().put("category",category).put("description",description).apply{assignmentId?.let{put("assignmentId",it)}}){it}
 private fun assignment(j:JSONObject)=RiderAssignment(j.optLong("id"),j.optLong("order_id"),j.optString("status"),j.optInt("version"),j.optString("pickup_name","THE GREEN SHOP"),j.optJSONObject("delivery_address")?.let{it.optString("formatted",it.optString("addressLine","Destination available in navigation"))}.orEmpty())
 private suspend fun<T>request(path:String,token:String,method:String,body:JSONObject?=null,parse:(JSONObject)->T):Result<T> = withContext(Dispatchers.IO){if(!isConfigured)return@withContext Result.failure(IllegalStateException("Set API_BASE_URL to the deployed HTTPS backend."));runCatching{val b=Request.Builder().url("$baseUrl/$path").header("Authorization","Bearer $token");val req=when(method){"POST"->b.post((body?:JSONObject()).toString().toRequestBody("application/json".toMediaType()));"PATCH"->b.patch((body?:JSONObject()).toString().toRequestBody("application/json".toMediaType()));else->b.get()}.build();client.newCall(req).execute().use{r->val raw=r.body?.string().orEmpty();if(!r.isSuccessful){val message=runCatching{JSONObject(raw).optString("message")}.getOrNull().orEmpty();throw RiderApiException(r.code,message.ifBlank{when(r.code){401->"Your secure session could not be refreshed. Sign in again.";403->"Approved delivery partner access is required.";409->"This delivery changed. Refresh and retry.";else->"Request failed (${r.code})."}})};parse(JSONObject(raw))}}}
}
private fun Long.toUtcIso8601():String=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US).apply{timeZone=TimeZone.getTimeZone("UTC")}.format(Date(this))
private fun JSONArray?.rows(block:(JSONObject)->RiderRow):List<RiderRow>{if(this==null)return emptyList();return(0 until length()).map{block(getJSONObject(it))}}
