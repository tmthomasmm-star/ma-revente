package com.marevente.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val Bg=Color(0xFFF4F5F7); private val Ink=Color(0xFF17191F); private val Muted=Color(0xFF69707D)
private val Brand=Color(0xFF176B55); private val Soft=Color(0xFFE8F3EF)

private data class StockItem(val id:Long,val ref:String,val name:String,val cost:Double,val location:String,val platform:String,val created:Long,var sold:Boolean=false)
private data class Sale(val id:Long,val itemIds:List<Long>,val amount:Double,val fees:Double,val packing:Double,val platform:String,val created:Long,var active:Boolean=true,var keepPacking:Boolean=false)
private data class Costs(val pouch:Double=.18,val label:Double=.04,val protection:Double=.20,val box:Double=.55)

private class Store(c:Context){
 private val p=c.getSharedPreferences("ma_revente_v1",Context.MODE_PRIVATE)
 fun next()=p.getInt("next",1); fun next(v:Int){p.edit().putInt("next",v).apply()}
fun items(): List<StockItem> = try{val a=JSONArray(p.getString("items","[]"));(0 until a.length()).map{i->a.getJSONObject(i).run{StockItem(getLong("id"),getString("ref"),getString("name"),getDouble("cost"),optString("location"),optString("platform"),getLong("created"),optBoolean("sold"))}}}catch(_:Exception){emptyList()}
 fun items(v:List<StockItem>){val a=JSONArray();v.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("ref",x.ref);put("name",x.name);put("cost",x.cost);put("location",x.location);put("platform",x.platform);put("created",x.created);put("sold",x.sold)})};p.edit().putString("items",a.toString()).apply()}
 fun sales(): List<Sale> = try{val a=JSONArray(p.getString("sales","[]"));(0 until a.length()).map{i->a.getJSONObject(i).run{val z=getJSONArray("ids");Sale(getLong("id"),(0 until z.length()).map{z.getLong(it)},getDouble("amount"),getDouble("fees"),getDouble("packing"),optString("platform"),getLong("created"),optBoolean("active",true),optBoolean("keep"))}}}catch(_:Exception){emptyList()}
 fun sales(v:List<Sale>){val a=JSONArray();v.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("ids",JSONArray(x.itemIds));put("amount",x.amount);put("fees",x.fees);put("packing",x.packing);put("platform",x.platform);put("created",x.created);put("active",x.active);put("keep",x.keepPacking)})};p.edit().putString("sales",a.toString()).apply()}
 fun costs()=Costs(p.getFloat("pouch",.18f).toDouble(),p.getFloat("label",.04f).toDouble(),p.getFloat("protection",.20f).toDouble(),p.getFloat("box",.55f).toDouble())
 fun costs(x:Costs){p.edit().putFloat("pouch",x.pouch.toFloat()).putFloat("label",x.label.toFloat()).putFloat("protection",x.protection.toFloat()).putFloat("box",x.box.toFloat()).apply()}
}
private enum class Screen{HOME,STOCK,ADD,SELL,SALES,SETTINGS}; private enum class Period{DAY,WEEK,MONTH}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}

@Composable private fun App(){
 val c=LocalContext.current;val store=remember{Store(c)};val stock=remember{mutableStateListOf<StockItem>().apply{addAll(store.items())}};val sales=remember{mutableStateListOf<Sale>().apply{addAll(store.sales())}}
 var costs by remember{mutableStateOf(store.costs())};var screen by remember{mutableStateOf(Screen.HOME)};var period by remember{mutableStateOf(Period.MONTH)};var note by remember{mutableStateOf("")}
 fun persist(){store.items(stock);store.sales(sales)}
 MaterialTheme(colorScheme=lightColorScheme(primary=Brand,background=Bg,surface=Color.White,onSurface=Ink)){Scaffold(containerColor=Bg,bottomBar={NavigationBar(containerColor=Color.White){listOf(Screen.HOME to "Accueil",Screen.STOCK to "Stock",Screen.ADD to "Ajouter",Screen.SELL to "Vendre",Screen.SALES to "Ventes").forEach{(s,l)->NavigationBarItem(selected=screen==s,onClick={screen=s;note=""},icon={Text(when(s){Screen.HOME->"⌂";Screen.STOCK->"▦";Screen.ADD->"＋";Screen.SELL->"€";else->"↶"},fontSize=20.sp)},label={Text(l,fontSize=10.sp)})}}}){pad->Column(Modifier.fillMaxSize().padding(pad)){Header{screen=Screen.SETTINGS};if(note.isNotBlank())Text(note,color=Brand,modifier=Modifier.padding(horizontal=16.dp,vertical=4.dp));when(screen){
  Screen.HOME->Home(stock,sales,period,{period=it},{screen=Screen.ADD},{screen=Screen.SELL},{screen=Screen.STOCK})
  Screen.STOCK->Stock(stock)
  Screen.ADD->Add(store,stock){persist();note=it;screen=Screen.STOCK}
  Screen.SELL->Sell(stock,costs){chosen,amount,fees,pack,platform->sales.add(0,Sale(System.currentTimeMillis(),chosen.map{it.id},amount,fees,pack,platform,System.currentTimeMillis()));chosen.forEach{it.sold=true};persist();note="Vente enregistrée : ${money(amount)}";screen=Screen.SALES}
  Screen.SALES->Sales(stock,sales){sale,keep->val i=sales.indexOfFirst{it.id==sale.id};if(i>=0)sales[i]=sale.copy(active=false,keepPacking=keep);sale.itemIds.forEach{id->stock.find{it.id==id}?.sold=false};persist();note="Retour effectué et chiffres recalculés";screen=Screen.HOME}
  Screen.SETTINGS->Settings(costs){costs=it;store.costs(it);note="Coûts enregistrés";screen=Screen.HOME}
 }}}}
}

@Composable private fun Header(settings:()->Unit){Row(Modifier.fillMaxWidth().padding(18.dp,14.dp,10.dp,7.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Ma Revente",fontSize=22.sp,fontWeight=FontWeight.Bold);Text("Simple, rapide, rentable",color=Muted,fontSize=12.sp)};TextButton(settings){Text("Réglages")}}}

@Composable private fun Home(stock:List<StockItem>,sales:List<Sale>,period:Period,setPeriod:(Period)->Unit,add:()->Unit,sell:()->Unit,openStock:()->Unit){val start=periodStart(period);val current=sales.filter{it.active&&it.created>=start};val map=stock.associateBy{it.id};val ca=current.sumOf{it.amount};val cost=current.sumOf{s->s.itemIds.sumOf{map[it]?.cost?:0.0}};val gross=ca-cost;val waste=sales.filter{!it.active&&it.keepPacking&&it.created>=start}.sumOf{it.packing};val gain=gross-current.sumOf{it.fees+it.packing}-waste
 LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Row(Modifier.fillMaxWidth().background(Color(0xFFE9EBEF),RoundedCornerShape(12.dp)).padding(4.dp)){Period.values().forEach{p->FilterChip(period==p,{setPeriod(p)},{Text(when(p){Period.DAY->"Aujourd’hui";Period.WEEK->"Semaine";Period.MONTH->"Mois"})},Modifier.weight(1f))}}};item{Card(colors=CardDefaults.cardColors(containerColor=Brand),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp)){Text("Chiffre d’affaires",color=Color.White.copy(.8f));Text(money(ca),color=Color.White,fontSize=38.sp,fontWeight=FontWeight.Bold);Text("${current.size} vente${if(current.size>1)"s"else""}",color=Color.White.copy(.8f))}}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Marge brute",gross,Modifier.weight(1f));Metric("Gain estimé",gain,Modifier.weight(1f))}};item{Text("Actions rapides",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Button(add,Modifier.weight(1f).height(58.dp)){Text("＋ Ajouter")};OutlinedButton(sell,Modifier.weight(1f).height(58.dp)){Text("€ Vendre")}}};item{Card(Modifier.fillMaxWidth().clickable{openStock()}){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${stock.count{!it.sold}} objets en stock",fontWeight=FontWeight.Bold);Text("${money(stock.filter{!it.sold}.sumOf{it.cost})} immobilisés",color=Muted)};Text("›",fontSize=28.sp)}}}}
}
@Composable private fun Metric(l:String,v:Double,m:Modifier){Card(m){Column(Modifier.padding(15.dp)){Text(l,color=Muted,fontSize=12.sp);Text(money(v),fontSize=21.sp,fontWeight=FontWeight.Bold)}}}

@Composable private fun Stock(stock:List<StockItem>){var q by remember{mutableStateOf("")};var f by remember{mutableStateOf("Tout")};val shown=stock.filter{!it.sold}.filter{f=="Tout"||(f=="À publier"&&it.platform.isBlank())||(f=="En vente"&&it.platform.isNotBlank())}.filter{q.isBlank()||listOf(it.ref,it.name,it.location,it.platform).any{x->x.contains(q,true)}};Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){Input("Numéro, nom, marque ou emplacement",q,{q=it});Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.padding(vertical=8.dp)){listOf("Tout","En vente","À publier").forEach{x->FilterChip(f==x,{f=x},{Text(x)})}};Text("${shown.size} objet${if(shown.size>1)"s"else""}",color=Muted,fontSize=12.sp);LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=8.dp)){items(shown,key={it.id}){StockRow(it)}}}}
@Composable private fun StockRow(x:StockItem,selected:Boolean=false,click:(()->Unit)?=null){Card(Modifier.fillMaxWidth().then(if(click!=null)Modifier.clickable{click()}else Modifier),colors=CardDefaults.cardColors(containerColor=if(selected)Soft else Color.White)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(50.dp).background(Color(0xFFE9EBEF),RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){Text("📦",fontSize=22.sp)};Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text("${x.ref} · ${x.name}",fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(listOf(x.location,x.platform).filter{it.isNotBlank()}.joinToString(" · ").ifBlank{"Non publié"},color=Muted,fontSize=12.sp)};Text(money(x.cost),fontWeight=FontWeight.Bold)}}}

@Composable private fun Add(store:Store,stock:SnapshotStateList<StockItem>,done:(String)->Unit){var name by remember{mutableStateOf("")};var cost by remember{mutableStateOf("")};var qty by remember{mutableStateOf("1")};var location by remember{mutableStateOf("")};var platform by remember{mutableStateOf("")};val n=store.next();LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Text("Ajouter au stock",fontSize=24.sp,fontWeight=FontWeight.Bold)};item{Card(Modifier.fillMaxWidth().height(74.dp),colors=CardDefaults.cardColors(containerColor=Soft)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("📷 Photo prévue dans la prochaine étape",color=Brand)}}};item{Input("Nom de l’objet",name,{name=it})};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){Input("Prix d’achat unitaire",cost,{cost=it},true)};Box(Modifier.width(105.dp)){Input("Quantité",qty,{qty=it},true)}}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){Input("Référence",ref(n),{},readOnly=true)};Box(Modifier.weight(1f)){Input("Emplacement",location,{location=it})}}};item{Input("Plateforme prévue (facultatif)",platform,{platform=it})};item{Button(onClick={val total=qty.toIntOrNull()?.coerceIn(1,100)?:1;val price=parse(cost);if(name.isNotBlank()){repeat(total){i->stock.add(0,StockItem(System.currentTimeMillis()+i,ref(n+i),name,price,location,platform,System.currentTimeMillis()))};store.next(n+total);done("$total article${if(total>1)"s"else""} ajouté${if(total>1)"s"else""}")}},modifier=Modifier.fillMaxWidth().height(54.dp)){Text("Ajouter au stock")}}}}

@Composable private fun Sell(stock:List<StockItem>,costs:Costs,save:(List<StockItem>,Double,Double,Double,String)->Unit){var q by remember{mutableStateOf("")};val selected=remember{mutableStateListOf<Long>()};var amount by remember{mutableStateOf("")};var fees by remember{mutableStateOf("0")};var platform by remember{mutableStateOf("Vinted")};var packType by remember{mutableStateOf("Vêtement")};var menu by remember{mutableStateOf(false)};val available=stock.filter{!it.sold};val matches=if(q.isBlank())emptyList()else available.filter{it.ref.removePrefix("N").trimStart('0').startsWith(q.trim().removePrefix("N"),true)||it.ref.contains(q,true)||it.name.contains(q,true)||it.location.contains(q,true)}.take(6);val chosen=selected.mapNotNull{id->available.find{it.id==id}};val pack=when(packType){"Fragile"->costs.pouch+costs.label+costs.protection;"Colis"->costs.box+costs.label+costs.protection;"Aucun"->0.0;else->costs.pouch+costs.label}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Text("Enregistrer une vente",fontSize=24.sp,fontWeight=FontWeight.Bold)};item{Input("Numéro, nom ou emplacement",q,{q=it})};items(matches,key={it.id}){x->StockRow(x,selected.contains(x.id)){if(!selected.contains(x.id))selected.add(x.id);q=""}};if(chosen.isNotEmpty()){item{Text("${chosen.size} article${if(chosen.size>1)"s"else""} · coût ${money(chosen.sumOf{it.cost})}",color=Brand,fontWeight=FontWeight.Bold)};items(chosen,key={"s${it.id}"}){x->StockRow(x,true){selected.remove(x.id)}};item{Input("Montant total reçu",amount,{amount=it},true)};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){Input("Plateforme",platform,{platform=it})};Box(Modifier.weight(1f)){Input("Frais",fees,{fees=it},true)}}};item{Box{OutlinedButton({menu=true},Modifier.fillMaxWidth()){Text("Emballage : $packType · ${money(pack)}")};DropdownMenu(menu,{menu=false}){listOf("Vêtement","Fragile","Colis","Aucun").forEach{x->DropdownMenuItem({Text(x)},{packType=x;menu=false})}}}};item{Button({if(parse(amount)>0)save(chosen,parse(amount),parse(fees),pack,platform)},Modifier.fillMaxWidth().height(54.dp)){Text("Confirmer la vente")}}}}}

@Composable private fun Sales(stock:List<StockItem>,sales:List<Sale>,cancel:(Sale,Boolean)->Unit){var target by remember{mutableStateOf<Sale?>(null)};LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Text("Historique des ventes",fontSize=24.sp,fontWeight=FontWeight.Bold)};items(sales,key={it.id}){s->val linked=s.itemIds.mapNotNull{id->stock.find{it.id==id}};Card(Modifier.fillMaxWidth().clickable{if(s.active)target=s}){Column(Modifier.padding(14.dp)){Row{Text(if(s.active)"${linked.size} article${if(linked.size>1)"s"else""} · ${s.platform}"else"Vente annulée",Modifier.weight(1f),fontWeight=FontWeight.Bold);Text(if(s.active)money(s.amount)else"—",color=if(s.active)Brand else Muted,fontWeight=FontWeight.Bold)};Text(linked.joinToString{it.ref},color=Muted,fontSize=12.sp);Text(SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.FRANCE).format(Date(s.created)),color=Muted,fontSize=12.sp)}}}};if(target!=null)AlertDialog(onDismissRequest={target=null},title={Text("Annulation ou retour")},text={Text("Réintégrer les articles au stock et recalculer les résultats ?")},confirmButton={Column{Button({cancel(target!!,true);target=null}){Text("Emballage utilisé")};TextButton({cancel(target!!,false);target=null}){Text("Emballage non utilisé")}}},dismissButton={TextButton({target=null}){Text("Retour")}})}

@Composable private fun Settings(x:Costs,save:(Costs)->Unit){var a by remember{mutableStateOf(x.pouch.toString())};var b by remember{mutableStateOf(x.label.toString())};var c by remember{mutableStateOf(x.protection.toString())};var d by remember{mutableStateOf(x.box.toString())};Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Coûts d’emballage",fontSize=24.sp,fontWeight=FontWeight.Bold);Text("À renseigner une seule fois.",color=Muted);Input("Pochette",a,{a=it},true);Input("Étiquette thermique",b,{b=it},true);Input("Protection",c,{c=it},true);Input("Carton",d,{d=it},true);Button({save(Costs(parse(a),parse(b),parse(c),parse(d)))},Modifier.fillMaxWidth()){Text("Enregistrer")}}}
@Composable private fun Input(label:String,value:String,change:(String)->Unit,numeric:Boolean=false,readOnly:Boolean=false){OutlinedTextField(value,change,label={Text(label)},singleLine=true,readOnly=readOnly,keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text),modifier=Modifier.fillMaxWidth())}

private fun money(v:Double)=NumberFormat.getCurrencyInstance(Locale.FRANCE).format(v);private fun parse(s:String)=s.replace("€","").replace(" ","").replace(",",".").toDoubleOrNull()?:0.0;private fun ref(n:Int)="N"+n.toString().padStart(4,'0')
private fun periodStart(p:Period):Long{val c=Calendar.getInstance();when(p){Period.DAY->{c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0)};Period.WEEK->{c.set(Calendar.DAY_OF_WEEK,c.firstDayOfWeek);c.set(Calendar.HOUR_OF_DAY,0)};Period.MONTH->{c.set(Calendar.DAY_OF_MONTH,1);c.set(Calendar.HOUR_OF_DAY,0)}};return c.timeInMillis}
