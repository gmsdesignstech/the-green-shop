package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.rider.*
import com.example.location.LocationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnerScreen(tokenProvider:suspend(Boolean)->Result<String>,onSignOut:()->Unit,repository:RiderRepository=remember{RiderRepository()}){
 val context=LocalContext.current;val scope=rememberCoroutineScope();val location=remember{LocationService.create(context)}
 var dashboard by remember{mutableStateOf<RiderDashboard?>(null)};var section by remember{mutableStateOf("Home")};var rows by remember{mutableStateOf<List<RiderRow>>(emptyList())};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)};var supportOpen by remember{mutableStateOf(false)}
 suspend fun load(){
  loading=true;error=null
  val token=tokenProvider(false).getOrElse{error=it.message;loading=false;return}
  suspend fun request(t:String)=if(section=="Home")repository.dashboard(t).map{dashboard=it;emptyList()}else repository.list(t,section)
  var result=request(token)
  if((result.exceptionOrNull() as? RiderApiException)?.statusCode==401){val refreshed=tokenProvider(true);result=refreshed.getOrNull()?.let{request(it)}?:Result.failure(refreshed.exceptionOrNull()!!)}
  result.fold({if(section!="Home")rows=it},{error=riderErrorMessage(it)});loading=false
 }
 LaunchedEffect(section){load()}
 fun availability(value:Boolean){val d=dashboard?:return;scope.launch{val token=tokenProvider(false).getOrElse{error=it.message;return@launch};busy=true;repository.availability(token,value,d.profile.availabilityVersion).fold({load()},{error=it.message});busy=false}}
 fun action(target:String,otp:String?=null){val a=dashboard?.active?:return;scope.launch{val token=tokenProvider(false).getOrElse{error=it.message;return@launch};busy=true;repository.action(token,a,target,otp).fold({load()},{error=it.message});busy=false}}
 Scaffold(topBar={TopAppBar(title={Column{Text("THE GREEN SHOP",fontWeight=FontWeight.Black);Text("Delivery Partner • Freshness At Your Doorstep",style=MaterialTheme.typography.labelSmall)}},actions={IconButton(onClick=onSignOut){Icon(Icons.AutoMirrored.Rounded.Logout,"Sign out")}})},bottomBar={NavigationBar{listOf("Home" to Icons.Rounded.Home,"History" to Icons.Rounded.History,"Earnings" to Icons.Rounded.CurrencyRupee,"Notifications" to Icons.Rounded.Notifications).forEach{(name,icon)->NavigationBarItem(selected=section==name,onClick={section=name},icon={Icon(icon,null)},label={Text(name)})}}}){padding->
  when{loading->RiderLoading(Modifier.padding(padding));error!=null->RiderError(error!!,{scope.launch{load()}},Modifier.padding(padding));section=="Home"->RiderHome(dashboard,busy,::availability,::action,onRetry={scope.launch{load()}},onNavigate={address->context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q=${Uri.encode(address)}")))},onLocation={val a=dashboard?.active;if(a!=null)scope.launch{val token=tokenProvider(false).getOrElse{error=it.message;return@launch};busy=true;location.getCurrentLocation().fold({point->repository.sendLocation(token,a.id,point).fold({},{error=it.message})},{error=it.message});busy=false}},onSupport={supportOpen=true},modifier=Modifier.padding(padding));else->RiderList(rows,section,Modifier.padding(padding))}
 }
 if(supportOpen)SupportDialog(onDismiss={supportOpen=false}){category,description->scope.launch{val token=tokenProvider(false).getOrElse{error=it.message;return@launch};repository.support(token,dashboard?.active?.id,category,description).fold({supportOpen=false},{error=it.message})}}
}

@Composable
private fun RiderHome(d:RiderDashboard?,busy:Boolean,onAvailability:(Boolean)->Unit,onAction:(String,String?)->Unit,onRetry:()->Unit,onNavigate:(String)->Unit,onLocation:()->Unit,onSupport:()->Unit,modifier:Modifier){
 if(d==null)return
 LazyColumn(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.2f)),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Text("Hello, ${d.profile.name}",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text("Approved partner • ${d.profile.serviceArea}",color=MaterialTheme.colorScheme.primary)}
  if(d.unavailableSections.isNotEmpty())item{
   ElevatedCard(shape=RoundedCornerShape(14.dp)){
    Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
     Text("Some dashboard data is temporarily unavailable. Retry to refresh.",Modifier.weight(1f),color=MaterialTheme.colorScheme.error)
     TextButton(onClick=onRetry,enabled=!busy){Text("Retry")}
    }
   }
  }
  item{ElevatedCard(shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(d.profile.online)"You’re online" else "You’re offline",fontWeight=FontWeight.Bold);Text(if(d.profile.online)"Ready for assigned deliveries" else "Location sharing is stopped",style=MaterialTheme.typography.bodySmall)};Switch(d.profile.online,{onAvailability(it)},enabled=!busy)}}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Completed today",d.summary.todayCompleted,Modifier.weight(1f));Metric("Today’s earnings",d.summary.todayEarnings.asRupees(),Modifier.weight(1f))}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Earned, unpaid",d.summary.pendingEarnings.asRupees(),Modifier.weight(1f));Metric("Pending payout",d.summary.pendingPayout.asRupees(),Modifier.weight(1f))}}
  item{Text("Active delivery",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
  if(d.active==null)item{ElevatedCard{Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.CheckCircle,null,tint=MaterialTheme.colorScheme.primary);Text("No active assignment",fontWeight=FontWeight.Bold);Text("Assigned deliveries will appear here.")}}}else item{AssignmentCard(d.active,busy,onAction,onNavigate,onLocation)}
  item{OutlinedButton(onClick=onSupport,modifier=Modifier.fillMaxWidth().height(52.dp)){Icon(Icons.Rounded.SupportAgent,null);Spacer(Modifier.width(8.dp));Text("Delivery support")}}
 }
}
private fun String.asRupees()=if(this=="—")this else "₹$this"
@Composable private fun Metric(label:String,value:String,modifier:Modifier)=ElevatedCard(modifier,shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(16.dp)){Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary);Text(label,style=MaterialTheme.typography.bodySmall)}}
@Composable
private fun AssignmentCard(a:RiderAssignment,busy:Boolean,onAction:(String,String?)->Unit,onNavigate:(String)->Unit,onLocation:()->Unit) {
    var otp by remember { mutableStateOf("") }
    val next = when(a.status) {
        "assigned" -> "accepted"
        "accepted" -> "arrived_pickup"
        "arrived_pickup" -> "picked_up"
        "picked_up" -> "out_for_delivery"
        "out_for_delivery" -> "delivered"
        else -> null
    }
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Order #${a.orderId}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Text(a.status.replace('_',' ').uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Rounded.DeliveryDining, null, tint = MaterialTheme.colorScheme.primary)
            }
            Text("Pickup: ${a.pickupName}")
            Text("Destination: ${a.destination.ifBlank { "Protected delivery address" }}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onNavigate(a.destination) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Navigation, null); Text(" Navigate")
                }
                if (a.status in listOf("picked_up","out_for_delivery")) {
                    OutlinedButton(onClick = onLocation, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.MyLocation, null); Text(" Share")
                    }
                }
            }
            if (a.status == "out_for_delivery") {
                OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(8) }, label = { Text("Customer delivery OTP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            next?.let { target ->
                Button(onClick = { onAction(target, otp.takeIf { it.isNotBlank() }) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text(when(target) { "accepted"->"Accept assignment"; "arrived_pickup"->"Arrived at pickup"; "picked_up"->"Confirm pickup"; "out_for_delivery"->"Start delivery"; else->"Verify OTP & complete" })
                }
            }
        }
    }
}
@Composable private fun RiderList(rows:List<RiderRow>,section:String,modifier:Modifier){if(rows.isEmpty()){Column(modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.Inbox,null);Text("No ${section.lowercase()} yet")};return};LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(rows,key={it.id}){r->ElevatedCard{Row(Modifier.fillMaxWidth().padding(16.dp)){Column(Modifier.weight(1f)){Text(r.title,fontWeight=FontWeight.Bold);Text(r.subtitle,style=MaterialTheme.typography.bodySmall)};Text(r.status.replace('_',' ').uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}}}}}
@Composable private fun RiderLoading(modifier:Modifier)=Box(modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
@Composable private fun RiderError(message:String,retry:()->Unit,modifier:Modifier)=Column(modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.CloudOff,null,tint=MaterialTheme.colorScheme.error);Text(message,modifier=Modifier.padding(16.dp));Button(onClick=retry){Text("Retry")}}
private fun riderErrorMessage(error:Throwable):String{
 if(error is RiderApiException&&error.statusCode>=500)return error.message?: "The delivery service is temporarily unavailable. Retry shortly."
 if(generateSequence(error){it.cause}.any{it is java.io.IOException})return "Cannot reach the delivery service. Check the phone’s network/API URL and tap Retry."
 return error.message?: "The delivery request failed. Retry."
}
@Composable private fun SupportDialog(onDismiss:()->Unit,onSubmit:(String,String)->Unit){var category by remember{mutableStateOf("other")};var description by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Delivery support")},text={Column{OutlinedTextField(category,{category=it},label={Text("Category")});OutlinedTextField(description,{description=it},label={Text("What happened?")},minLines=3)}},confirmButton={Button(onClick={onSubmit(category,description)},enabled=description.length>=5){Text("Submit")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}
