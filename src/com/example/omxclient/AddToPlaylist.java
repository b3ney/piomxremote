package com.example.omxclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.example.omxclient.ChooseFile.PlayFile;
import com.example.omxclient.ChooseFile.ReadReturnFIleThread;
import com.example.omxclient.RemoteControl.ClientThread;
import com.example.omxclient.RemoteControl.ReadReturnThread;
import com.example.omxclient.RemoteControl.SendKeyThread;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class AddToPlaylist extends Activity {
	
	private Socket socket;

	private  int SERVERPORT = 3234;
	private String SERVER_IP = "192.168.0.31";
	
	private PrintWriter out = null;
    private BufferedReader in = null;
    
    private SocketService mMyService;
    
    public int maxIdPlaylist=0;
    
    public boolean removeAfterRead=false;
    public boolean playLoop=false;
    
    public static final int MENU_FILE = Menu.FIRST + 3;
	public static final int MENU_PARAM = Menu.FIRST + 2;
	public static final int MENU_PARAM_PLAYLIST = Menu.FIRST ;
	public static final int MENU_ARRETE = Menu.FIRST+4 ;
	public static final int MENU_CLEAR = Menu.FIRST+5 ;
	public static final int MENU_SAVE = Menu.FIRST+6 ;
	public static final int MENU_REMOTE = Menu.FIRST+1 ;
   public int myLastUpdate=0;
    ListView playlistView ;
    SimpleAdapter mSchedule;
    checkServiveUpdate chsu;
    ArrayList<HashMap<String, String>>  listItem = new ArrayList<HashMap<String, String>>();
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_PARAM_PLAYLIST, Menu.NONE, "Option");
        menu.add(Menu.NONE, MENU_REMOTE, Menu.NONE, "Remote");
        menu.add(Menu.NONE, MENU_PARAM, Menu.NONE, "Parametres");
        menu.add(Menu.NONE, MENU_FILE, Menu.NONE, "File...");
        menu.add(Menu.NONE, MENU_ARRETE, Menu.NONE, "Arreter la lecture");
        menu.add(Menu.NONE, MENU_CLEAR, Menu.NONE, "Vider la playlist");
        menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, "Enregistrer");
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case MENU_PARAM:
            	startActivity(new Intent(this, SettingActivity.class));    
            return true;
            case MENU_REMOTE:
        		startActivity(new Intent(this, RemoteControl.class));     	
            return true;
            case MENU_FILE:
            	startActivity(new Intent(this, ChooseFile.class)); 	
            return true;
            case MENU_PARAM_PLAYLIST:
            	OptionParam();
            return true;
            case MENU_ARRETE:
            	stopPlaylist();
            return true;
            case MENU_CLEAR:
            	clearPlaylist();
            return true;
            case MENU_SAVE:
            	savePlaylist();
            return true;
            default:
            	return super.onOptionsItemSelected(item);
        }
    }
    
    public void loadPref()
    {
    	SharedPreferences sharedPref =this.getSharedPreferences("OMXclient",Context.MODE_PRIVATE);
		this.SERVER_IP=sharedPref.getString("IP", "192.168.0.1");
		this.SERVERPORT=sharedPref.getInt("PORT", 3234);
    }
    
	public void ifIntent()
    {
    	
    	Bundle extras = getIntent().getExtras();
		String youtubeSharing = null;
		 
		 if(extras!=null && extras.getString(Intent.EXTRA_TEXT)!=null)
		 	youtubeSharing =extras.getString(Intent.EXTRA_TEXT);
		//sharing youtube :
		 
			if(youtubeSharing!=null)
			{
				final String yt=youtubeSharing;
				
                 SendKeyThread skt=new SendKeyThread("");

				skt.setKey("ADDPLAYLIST|youtube|"+yt+"|caSertArien");
				new Thread(skt).start();
				showToast(youtubeSharing);
			
				
			}
    }
	@Override
	protected void onPause()
	{
		super.onPause();
		if(chsu!=null)
			chsu.running=false;
		//doUnbindService();
	}
	@Override
	protected void onStop()
	{
		super.onStop();
		if(chsu!=null)
			chsu.running=false;
		//doUnbindService();
	}
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if(chsu!=null)
			chsu.running=false;
		doUnbindService();
	}
	@Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first
	    if(chsu==null || !chsu.running)
	    {  chsu= new checkServiveUpdate();
		 	new Thread(chsu).start();	
	    }
	}
	 @Override
	    public boolean onKeyDown(int keyCode, KeyEvent event) {
	        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
	        	final SendKeyThread skt=new SendKeyThread("");
	        	skt.setKey("key|MOINS");
	        	new Thread(skt).start();
	        	return true;
	        	
	        }else if((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
	        	final SendKeyThread skt=new SendKeyThread("");
	        	skt.setKey("key|PLUS");
	        	new Thread(skt).start();
	        	return true;
	        }else if((keyCode == KeyEvent.KEYCODE_BACK)){
	        	AddToPlaylist.this.finish();
	        	return true;
	        	
	        }else {
	        	return false;
	        }
	        
	    }
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_to_playlist);
		doBindService();
		//Intent i = new Intent(AddToPlaylist.this, SocketService.class);
		 //startService(i);
		 
		loadPref();
		
		ifIntent();
		//return;
		
		 playlistView = (ListView) findViewById(R.id.listView1);
		 this.setTitle("Playlist : ");
		// Defined Array values to show in ListView
		 listItem = new ArrayList<HashMap<String, String>>();
		 mSchedule = new SimpleAdapter (AddToPlaylist.this.getBaseContext(), listItem, R.layout.affichageitem,
			        new String[] {"img", "titre", "description"}, new int[] {R.id.img, R.id.titre, R.id.description});
		 playlistView.setAdapter(mSchedule);
		 final Context mm=this;
		 playlistView.setOnItemClickListener(new OnItemClickListener() {			

	 			@Override
	 			public void onItemClick(AdapterView<?> arg0, View arg1, int position,long arg3) {
	 				// TODO Auto-generated method stub
	 				final HashMap<String, String> map = (HashMap<String, String>) playlistView.getItemAtPosition(position);
	 				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	 				    @Override
	 				    public void onClick(DialogInterface dialog, int which) {
	 				        switch (which){
	 				        case DialogInterface.BUTTON_POSITIVE:
	 			 				String t[] =  map.get("titre").split("-");
	 			 				String action[] =  map.get("description").split("-");
	 			 				SendKeyThread skt=new SendKeyThread("");
	 			 				if(action[0].trim().equals("Stop"))
	 			 				{	
	 			 					skt.setKey("key|q");
	 			 					new Thread(skt).start();
	 			 				}else if(action[0].trim().equals("Remove"))
	 			 				{	
	 			 					skt.setKey("REMOVEPLAYLIST|"+t[0].trim());
	 			 					new Thread(skt).start();
	 			 				}
	 			 				
	 				            break;

	 				        case DialogInterface.BUTTON_NEGATIVE:
	 				            //No button clicked
	 				            break;
	 				        }
	 				    }
	 				};

	 				AlertDialog.Builder builder = new AlertDialog.Builder(mm);
	 				builder.setMessage("Voulez vous retirer ce morceau de la playlist ?").setPositiveButton("Oui", dialogClickListener)
	 				    .setNegativeButton("Non", dialogClickListener).show();
	 			}
		 });
			  
		 playlistView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

	             @Override
	             public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
	            	 final HashMap<String, String> map = (HashMap<String, String>) playlistView.getItemAtPosition(position);
	            	 String t[] =  map.get("titre").split("-");
	            	 String action[] =  map.get("description").split("-");
	         		
	         		if(action[0].trim().equals("Remove"))
		 			{	
	         			SendKeyThread skt=new SendKeyThread("");
		 					skt.setKey("GOTOPLAYLIST|"+t[0].trim());
		 					new Thread(skt).start();
		 			}
	            	 return true;
	             }

	           });
		 		 
		 if(chsu==null || !chsu.running)
		    {  chsu= new checkServiveUpdate();
			 	new Thread(chsu).start();	
		    }		 
	}
	
	/*public void OptionParam_old()
	{
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			    	SendKeyThread skt=new SendKeyThread("");
			        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:			        	
	 					skt.setKey("SETPARAMPLAYLIST|LIREENBOUCLE=0|REMOVEAFTER=1");
	 					new Thread(skt).start();		
			            break;

			        case DialogInterface.BUTTON_NEGATIVE:
			        	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						    @Override
						    public void onClick(DialogInterface dialog, int which) {
						    	SendKeyThread skt=new SendKeyThread("");
						        switch (which){
						        case DialogInterface.BUTTON_POSITIVE:			        	
				 					skt.setKey("SETPARAMPLAYLIST|LIREENBOUCLE=1|REMOVEAFTER=0");
				 					new Thread(skt).start();		
						            break;

						        case DialogInterface.BUTTON_NEGATIVE:
						        	skt.setKey("SETPARAMPLAYLIST|LIREENBOUCLE=0|REMOVEAFTER=0");
				 					new Thread(skt).start();		
						            break;
						        }
						    }
						};

						AlertDialog.Builder builder = new AlertDialog.Builder(AddToPlaylist.this);
						builder.setMessage("Lire en boucle ?").setPositiveButton("Oui", dialogClickListener)
						    .setNegativeButton("Non", dialogClickListener).show();
			            break;
			        }
			    }
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Retirer les �l�ment apr�s lecture ?").setPositiveButton("Oui", dialogClickListener)
			    .setNegativeButton("Non", dialogClickListener).show();
		
	}*/
	
	public void OptionParam()
	{
		String[] lesOption={"Retirer les �l�ments apr�s lecture","Lire en boucle"};
		final boolean[] lesCheck= {removeAfterRead,playLoop};
		//final ArrayList mSelectedItems = new ArrayList();  // Where we track the selected items
		    AlertDialog.Builder builder = new AlertDialog.Builder(AddToPlaylist.this);
		    // Set the dialog title
		    builder.setTitle("Option de la playlist")		    
		           .setMultiChoiceItems(lesOption, lesCheck,
		                      new DialogInterface.OnMultiChoiceClickListener() {
		               @Override
		               public void onClick(DialogInterface dialog, int which,
		                       boolean isChecked) {
		                   if (isChecked) {
		                       // If the user checked the item, add it to the selected items
		                       //mSelectedItems.add(which);
		                	   lesCheck[which]=true;
		                	   //On decoche lire en boucle si "Retirer" est checked :
		                	   if(which==0){
		                		   lesCheck[1]=false;
		                		   ((AlertDialog) dialog).getListView().setItemChecked(1, false);
		                	   }
		                	   //On d�codehe "retirer" si lire en boucle est checked
		                	   if(which==1){
		                		   lesCheck[0]=false;
		                		   ((AlertDialog) dialog).getListView().setItemChecked(0, false);
		                	   }
		                   } else {
		                	   lesCheck[which]=false;
		                   }
		               }
		           })
		    // Set the action buttons
		           .setPositiveButton("Valider", new DialogInterface.OnClickListener() {
		               @Override
		               public void onClick(DialogInterface dialog, int id) {
		            	   SendKeyThread skt=new SendKeyThread("");
		            	   showToast("SETPARAMPLAYLIST|LIREENBOUCLE="+(lesCheck[1]?"1":"0")+"|REMOVEAFTER="+(lesCheck[0]?"1":"0")); 
		            	   skt.setKey("SETPARAMPLAYLIST|LIREENBOUCLE="+(lesCheck[1]?"1":"0")+"|REMOVEAFTER="+(lesCheck[0]?"1":"0"));
		 					new Thread(skt).start();	
		               }
		           })
		           .setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
		               @Override
		               public void onClick(DialogInterface dialog, int id) {
		                  // ...
		               }
		           });

		    builder.create().show();

		
	}
	public void clearPlaylist()
	{
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:
			    		SendKeyThread skt=new SendKeyThread("");
			    		skt.setKey("SETPARAMPLAYLIST|LIREENBOUCLE=0|INUTILE=0");
			    		new Thread(skt).start();
			    		skt.setKey("REMOVEPLAYLIST|ALL");
			    		new Thread(skt).start();
			    		skt.setKey("key|q");
			    		new Thread(skt).start();
			            break;

			        case DialogInterface.BUTTON_NEGATIVE:
			            //No button clicked
			            break;
			        }
			    }
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Retirer tout les morceaux ?").setPositiveButton("Confirmer", dialogClickListener)
			    .setNegativeButton("Annuler", dialogClickListener).show();

	}
	public void stopPlaylist()
	{
		SendKeyThread skt=new SendKeyThread("");
		skt.setKey("SETPARAMPLAYLIST|LIREENBOUCLE=0|INUTILE=0");
		new Thread(skt).start();
		skt.setKey("GOTOPLAYLIST|"+(maxIdPlaylist+10));
		new Thread(skt).start();
		skt.setKey("key|q");
		new Thread(skt).start();
	}
	
	public void savePlaylist()
	{
		final EditText input = new EditText(this);
		input.setText("d");
   	 	new AlertDialog.Builder(this)
   	    .setTitle("Choisir un nom pour la playlist")
   	    .setMessage("Si une PL du m�me nom existe d�ja elle sera remplac� :")
   	    .setView(input)
   	    .setPositiveButton("Enregistrer", new DialogInterface.OnClickListener() {
   	        public void onClick(DialogInterface dialog, int whichButton) {
   	            String value = input.getText().toString(); 
   	            SendKeyThread skt=new SendKeyThread("");
   	            skt.setKey("SAVEPLAYLIST|"+value.replaceAll("/", ""));
   	            new Thread(skt).start();
   	        }
   	    }).setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
   	        public void onClick(DialogInterface dialog, int whichButton) {
   	        	return;
   	        }
   	    }).show();		
	}
	
	@Override
    public void onNewIntent (Intent intent)
    {
		loadPref();
	//	new Thread(new ClientThread()).start();
    	setIntent(intent);
    	ifIntent();
    }
	
	
	
	public void showToast(final String toast)
	{
	    runOnUiThread(new Runnable() {
	        public void run()
	        {
	            Toast.makeText(AddToPlaylist.this, toast, Toast.LENGTH_SHORT).show();
	        }
	    });
	}

	
	private ServiceConnection mConnection = new ServiceConnection() {
		 @Override
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        mMyService = ((SocketService.ServiceBinder)service).getService();
	    }
	    @Override
	    public void onServiceDisconnected(ComponentName className) {
	        mMyService = null;
	    }
		};


	void doBindService() {
	    bindService(new Intent(this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
	    // Detach our existing connection.
		 mMyService = null;
	    unbindService(mConnection);
	}
	
	public void listRefresh2()
	{
		 runOnUiThread(new Runnable() {
		        public void run()
		        {
		        	 mSchedule.notifyDataSetChanged();
		        }
		    });
		 
	}
	
	
	public void listRefresh3( final HashMap<String, String> map)
	{
		 runOnUiThread(new Runnable() {
		        public void run()
		        {
		        	listItem.add(map);		        	
		        }
		    });
		 
	}
class checkServiveUpdate implements Runnable {
	volatile boolean running = true;
	@Override
	public void run() {
		while(running)
		{
			doBindService();
			if(mMyService!=null &&  myLastUpdate!=mMyService.lastUpdate)
			{
				
				listItem.clear();
				//showToast("updating..."+mMyService.lastUpdate);
				myLastUpdate=mMyService.lastUpdate;
				  //	On d�clare la HashMap qui contiendra les informations pour un item
				  HashMap<String, String> map;
				 int idebug=0;
				 //plus simple pour le tri :
				 SortedSet<Integer> keysTrie = new TreeSet<Integer>( mMyService.lesMorceau.keySet());
								 
				 for (Integer ikey : keysTrie) {
					    map = new HashMap<String, String>();
						String key=ikey+"";
						String value=(String)mMyService.lesMorceau.get(ikey);
						 map = new HashMap<String, String>();
						 map.put("titre", key+"- "+value);
						    if(key.equals(mMyService.encour))
						    {
						    	map.put("description", "Stop- LECTURE EN COUR...");
						    	map.put("img", String.valueOf(R.drawable.bouleverte));			    	
						    }else{
						    	map.put("description", "Remove- Click to remove");
						    	map.put("img", String.valueOf(R.drawable.boulejaune));
						    }
					  		//	enfin on ajoute cette hashMap dans la arrayList
						    listRefresh3(map);
						    idebug++;
						    maxIdPlaylist=ikey;	 
					}
				 
		/*		  Iterator myVeryOwnIterator = mMyService.lesMorceau.keySet().iterator();
				  while(myVeryOwnIterator.hasNext()) 
				  {
					  //Cr�ation d'une HashMap pour ins�rer les informations du premier item de notre listView
					  map = new HashMap<String, String>();
					  String key=(String)myVeryOwnIterator.next();
					    String value=(String)mMyService.lesMorceau.get(key);
					    map.put("titre", key+"- "+value);
					    if(key.equals(mMyService.encour))
					    {
					    	map.put("description", "Stop- LECTURE EN COUR...");
					    	map.put("img", String.valueOf(R.drawable.bouleverte));			    	
					    }else{
					    	map.put("description", "Remove- Click to remove");
					    	map.put("img", String.valueOf(R.drawable.boulejaune));
					    }
				  		//	enfin on ajoute cette hashMap dans la arrayList
					    listRefresh3(map);
					    idebug++;
				  }*/
				 playLoop=mMyService.PARAM_LireEnBouble;
				 removeAfterRead=mMyService.PARAM_RemoveAfter;
				 String param=(mMyService.PARAM_LireEnBouble?"[LECTURE EN BOUCLE]":"[Keep after read : "+(mMyService.PARAM_RemoveAfter?"NON":"OUI")+"]");
				  showToast(idebug+" files in playlist\n"+param);
				  listRefresh2();
			}else{
				try {
					 Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
	
class ReadReturnThread implements Runnable {
	String retour="NO";
	@Override
	public void run() {

		
			try {
				String r;
				while((r= in.readLine())!=null)
				{	retour+=" "+r;
					if (r.trim().equals("OKEND"))
						break;
				}
			} catch (IOException e1) {
				retour="ERREUR";
				e1.printStackTrace();
				showToast(e1.toString());
			}
			
			showToast(retour);

	}

}

class SendKeyThread implements Runnable {

	String key="";
	public SendKeyThread(String pkey)
	{
		this.key=pkey;
	}
	
	public void setKey(String laKey)
	{
		
		this.key=laKey;
	}
	@Override
	public void run() {

		try {
    		if(socket==null)
    			{
    			InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
    			socket = new Socket(serverAddr, SERVERPORT);
		
    			}
    		try {
    			if(out==null)
    				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
				if(in==null)
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
			} catch (IOException e1) {
				e1.printStackTrace();
				showToast( e1.toString());
			}
    		
    		///new Thread(new ReadReturnThread()).start();
    		
    		if(socket==null)
    			showToast("Pb socket");
    			
    		else{
    			        			
    			out.println(this.key);
    			out.flush();
    			            			
    			new Thread(new ReadReturnThread()).start();
    			//et.setText("key|");
    			//startActivity(new Intent(null, RemoteControl.class));
			}

		} catch (Exception e) {
			showToast( e.toString());
			e.printStackTrace();
  		}

	}

}

}

