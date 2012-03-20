package com.MobiTrade;


import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import com.MobiTrade.R;
import com.MobiTrade.objectmodel.Channel;
import com.MobiTrade.objectmodel.Content;
import com.MobiTrade.sqlite.DatabaseHelper;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class FileExplorer extends ListActivity  {

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(item != null)
			item.clear();
		if(path != null)
			path.clear();
	}

	private List<String> item = null;
	private List<String> path = null;
	private String root="/sdcard";
	private TextView myPath;
	private final static String[] supportedFileExtensions = {"mp3", "txt", "jpg"};
	// The channel keywords within wich the content will be published
	private String selectedChannel;
	TextView channelDesc;
	private File selectedFile;
	private String selectedContentDescription;
	private int mYear;
	private int mMonth;
	private int mDay;
	private StringBuilder selectedExpirationDate;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Extract the extrat details received throught the Intent
		Bundle extras = getIntent().getExtras(); 
		selectedChannel = extras.getString(Channel.CHANNEL_KEYWORDS); 

		setContentView(R.layout.explorer);

		// The channel description textview
		channelDesc = (TextView)findViewById(R.id.channelDesc);
		channelDesc.setText("Selected Channel: " + selectedChannel);

		// The path description textview
		myPath = (TextView)findViewById(R.id.path);

		// get the current date
		final Calendar cal = Calendar.getInstance();
		mYear = cal.get(Calendar.YEAR);
		mMonth = cal.get(Calendar.MONTH);
		mDay = cal.get(Calendar.DAY_OF_MONTH);

		getDir(root);
	}



	private void getDir(String dirPath)
	{
		myPath.setText("Current Location: " + dirPath);

		item = new ArrayList<String>();
		path = new ArrayList<String>();

		File f = new File(dirPath);

		File[] files = f.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname)
			{
				//If a file or directory is hidden, or unreadable, don't show it in the list.
				if(pathname.isHidden())
					return false;

				if(!pathname.canRead())
					return false;

				//Show all directories in the list.
				if(pathname.isDirectory())
					return true;

				//Check if there is a supported file type that we can read.
				String fileName = pathname.getName();
				String fileExtension;
				int mid= fileName.lastIndexOf(".");
				fileExtension = fileName.substring(mid+1,fileName.length());
				for(String s : supportedFileExtensions) {
					if(s.contentEquals(fileExtension)) {
						return true;
					}
				}

				return false;
			}

		});

		if(!dirPath.equals(root))
		{

			item.add(root);
			path.add(root);

			item.add("../");
			path.add(f.getParent());

		}

		for(int i=0; i < files.length; i++)
		{
			File file = files[i];
			path.add(file.getPath());
			if(file.isDirectory())
				item.add(file.getName() + "/");
			else
				item.add(file.getName());
		}

		// We can modify this adapter in order to add icons to the files
		ArrayAdapter<String> fileList =
			new ArrayAdapter<String>(this, R.layout.row, item);
		setListAdapter(fileList);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		selectedFile = new File(path.get(position));

		if (selectedFile.isDirectory())
		{
			if(selectedFile.canRead())
				getDir(path.get(position));
			else
			{
				new AlertDialog.Builder(getParent())
				.setIcon(R.drawable.mobitradeicon)
				.setTitle("[" + selectedFile.getName() + "] folder can't be read!")
				.setPositiveButton("OK", 
						new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Nothing to do
					}
				}).show();
			}
		}
		else
		{
			new AlertDialog.Builder(getParent())
			.setIcon(R.drawable.mobitradeicon)
			.setTitle("Publishing : " + selectedFile.getName())
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// We cancel the publishing process, nothing to do
				}
			})

			.setPositiveButton("OK", 
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Verifying wether the selected file is readable or not
					if(selectedFile.canRead())
					{
						// Asking the Database Helper to publish the content and to copy it to the app directory
						if(DatabaseHelper.CopyFileToMobiTradeSDcard(selectedFile.getAbsolutePath(), selectedFile.getName(), selectedFile.length()))
						{
							// Ask the user to provide a description for the content, and ttl
							AlertDialog.Builder alert = new AlertDialog.Builder(getParent());
							final EditText input = new EditText(getParent());
							input.setSingleLine(false);
							alert.setIcon(R.drawable.mobitradeicon);
							alert.setView(input);
							alert.setTitle("Content description:");
							alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// Saving the selectedContentDescription and Asking for a time to live
									selectedContentDescription = input.getText().toString().trim();
									if(selectedContentDescription.length() > 0)
									{
										if(!DatabaseHelper.getDBManager().isContentAvailable(selectedContentDescription))
										{
											// Showing the Datepicker dialog
											Dialog date = new DatePickerDialog(getParent(), mDateSetListener, mYear, mMonth, mDay);
											date.setTitle("Choose an expiration date for this content (Optional)");
											date.setOnCancelListener(new DatePickerDialog.OnCancelListener() {

												@Override
												public void onCancel(DialogInterface dialogw) {
													// TODO Auto-generated method stub

													// The DatePickerDialog is canceled and 
													// null ttl has been set
													String fileExtension = selectedFile.getAbsolutePath().substring(selectedFile.getAbsolutePath().lastIndexOf("."));

													// We have the expiration date, so add the content to the database
													if(DatabaseHelper.getDBManager().insertContent(selectedContentDescription, BluetoothAdapter.getDefaultAdapter().getName(), DatabaseHelper.mobiTradeSdcardPath + selectedFile.getName(), "", "", 
															Content.CONTENT_DEFAULT_UTILITY, selectedChannel, DatabaseHelper.GetCurrentDate(), fileExtension, (int)selectedFile.length(), selectedFile.getName()) == -1)
													{
														Toast.makeText(getParent(), "A content with description: " + selectedContentDescription + "is already available.", Toast.LENGTH_LONG).show();
													}

													// Close the file explorer activity and go back to Dashboard
													finish();


												}});
											date.show();
										}else
										{
											// A content with the current description is already avilable
											Toast.makeText(FileExplorer.this, "A content with the selected description is already available. Please" +
													"provide a different description.", Toast.LENGTH_LONG).show();	
											// Delete the copied file from the MobiTrade directory
											DatabaseHelper.DeleteMobiTradeFile(DatabaseHelper.mobiTradeSdcardPath + selectedFile.getName());											
										}
									}else
									{
										// A description should be provided to the selected content, otherwise it will not be published
										Toast.makeText(FileExplorer.this, "Please provide a description for the selected content. Publishing process cancelled.", Toast.LENGTH_LONG).show();
										// Delete the copied file from the mobitrade directory
										DatabaseHelper.DeleteMobiTradeFile(DatabaseHelper.mobiTradeSdcardPath + selectedFile.getName());											
									}
								}
							});

							alert.setNegativeButton("Cancel",
									new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									dialog.cancel();
								}
							});
							alert.show();	

						}else
						{
							// problem occured when trying to copy the selected file to the MobiTrade SDcard
							Toast.makeText(FileExplorer.this, "A problem occured while trying to publish the selected" +
									"content, process cancelled.", Toast.LENGTH_LONG).show();
						}
					}else
					{
						// Cannot read the file 
						Toast.makeText(FileExplorer.this, "Unable to read the selectedd file, process cancelled.", Toast.LENGTH_LONG).show();
					}
				}
			}).show();

		}
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, 
				int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			selectedExpirationDate = new StringBuilder()
			.append(mDay).append("/")
			.append(mMonth + 1).append("/")
			.append(mYear);

			// The expiration date should not be before the current one
			Date curerentdate = new Date();
			Date expDate = new Date(mYear - 1900, mMonth, mDay);
			
			if(expDate.after(curerentdate))
			{

				String fileExtension = selectedFile.getAbsolutePath().substring(selectedFile.getAbsolutePath().lastIndexOf("."));
				// We have the expiration date, so add the content to the database
				if(DatabaseHelper.getDBManager().insertContent(selectedContentDescription, BluetoothAdapter.getDefaultAdapter().getName(), DatabaseHelper.mobiTradeSdcardPath + selectedFile.getName(), "", selectedExpirationDate.toString(), 
						Content.CONTENT_DEFAULT_UTILITY, selectedChannel, DatabaseHelper.GetCurrentDate(), fileExtension, (int)selectedFile.length(), selectedFile.getName()) == -1)
				{
					Toast.makeText(getParent(), "A content with description: " + selectedContentDescription + "is already available.", Toast.LENGTH_LONG).show();
				}

				// Close the file explorer activity and go back to Dashboard
				finish();

			}else
			{
				// The selected expiration date is invalid
				Toast.makeText(getParent(), "The selected expiration date: "+ selectedExpirationDate + "is invalid. Publishing process cancelled.", Toast.LENGTH_LONG).show();
				finish();
			}
		}
	};

}