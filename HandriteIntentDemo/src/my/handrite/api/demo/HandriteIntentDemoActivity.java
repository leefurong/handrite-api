package my.handrite.api.demo;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import my.handrite.api.HandriteIntent;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class HandriteIntentDemoActivity extends Activity {
	private final static String[] HANDRITE_PACKAGE_NAMES = new String[] {
			"my.handrite.prem", "my.handrite" };
	private static final String ACTION_FOR_START_HANDRITE = android.content.Intent.ACTION_EDIT;

	private static final Uri DEFAULT_NOTE_URI = Uri
			.fromFile(new File(Environment.getExternalStorageDirectory(),
					"handrite_api_demo.note"));
	private static final File EXPORT_FILE = new File(
			Environment.getExternalStorageDirectory(), "handrite_api_demo.png");
	private static final Uri EXPORT_URI = Uri.fromFile(EXPORT_FILE);
	private static final int REQUEST_CODE_START_HANDRITE = 0;
	private static final int REQUEST_CODE_SELECT_LOCAL_PICTURE = 1;
	private static final int TEST_WIDTH = 600;

	private CheckBox checkBoxAppendText;
	private CheckBox checkBoxAppendImage;
	private CheckBox checkBoxSetLabel;

	private TextView textViewAppendText;
	private TextView textViewAppendImageUri;
	private TextView textViewLabel;

	private Button btnStarthandrite;
	private ImageView noteImage;
	private Uri noteUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_handrite_intent_demo);
		findViews();
		setListeners();
		noteUri = DEFAULT_NOTE_URI;
	}

	private void findViews() {
		checkBoxAppendText = (CheckBox) findViewById(R.id.enableTextAppend);
		checkBoxAppendImage = (CheckBox) findViewById(R.id.enableImageAppend);
		checkBoxSetLabel = (CheckBox) findViewById(R.id.enableSetLabel);
		textViewAppendText = (TextView) findViewById(R.id.textForAppend);
		textViewAppendImageUri = (TextView) findViewById(R.id.imageAppendUri);
		textViewLabel = (TextView) findViewById(R.id.label);
		btnStarthandrite = (Button) findViewById(R.id.btnStartHandrite);
		noteImage = (ImageView) findViewById(R.id.noteImage);
	}

	private void setListeners() {
		btnStarthandrite.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startHandrite();
			}
		});
		checkBoxAppendImage
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked && !isValidLocalImageUriForAppend()) {
							selectLocalImage();
						}
					}
				});
		textViewAppendImageUri.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectLocalImage();
				checkBoxAppendImage.setChecked(true);
			}
		});
	}

	protected boolean isValidLocalImageUriForAppend() {
		File file = null;
		try {
			CharSequence cs = textViewAppendImageUri.getText();
			if (cs != null) {
				String s = cs.toString();
				file = imageUriToFile(this, Uri.parse(s));
			}
		} catch (URISyntaxException e) {
			file = null;
		}
		return file != null && file.exists();
	}

	protected void selectLocalImage() {
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.setType("image/png");
		startActivityForResult(i, REQUEST_CODE_SELECT_LOCAL_PICTURE);
	}

	private void startHandrite() {
		String packageName = getHandritePackageName();
		if (packageName.equals("")) {
			promptInstallHandrite();
		} else {
			Intent intent = new Intent(ACTION_FOR_START_HANDRITE)
					.setDataAndType(noteUri, "").setPackage(packageName);
			setExtras(intent);
			try {
				startActivityForResult(intent, REQUEST_CODE_START_HANDRITE);
			} catch (ActivityNotFoundException e) {
				promptLowVersion();
			}
		}
	}

	private void setExtras(Intent intent) {
		if (checkBoxAppendText.isChecked()) {
			CharSequence text = textViewAppendText.getText();
			intent.putExtra(HandriteIntent.EXTRA_TEXT, text);
		}
		if (checkBoxAppendImage.isChecked()) {
			Uri imageUri = Uri.parse(textViewAppendImageUri.getText()
					.toString());
			intent.putExtra(HandriteIntent.EXTRA_STREAM, imageUri);
		}
		if (checkBoxSetLabel.isChecked()) {
			intent.putExtra(HandriteIntent.EXTRA_LABELS, textViewLabel
					.getText().toString());
		}
		intent.putExtra(HandriteIntent.EXTRA_EXPORT, EXPORT_URI);
		intent.putExtra(HandriteIntent.EXTRA_EXPORT_WIDTH, TEST_WIDTH);
	}

	private void promptInstallHandrite() {
		Toast.makeText(this,
				"Please install Handrite for handwriting note taking",
				Toast.LENGTH_LONG).show();
	}

	private void promptLowVersion() {
		Toast.makeText(this,
				"Your Handrite is old, please update it to 1.81+.",
				Toast.LENGTH_LONG).show();
	}

	private String getHandritePackageName() {
		PackageManager packageManager = getPackageManager();
		for (String name : HANDRITE_PACKAGE_NAMES) {
			try {
				packageManager.getApplicationInfo(name, 0);
				return name;
			} catch (NameNotFoundException e) {
			}
		}
		return "";
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_START_HANDRITE:
			if (resultCode == RESULT_OK) {
				noteUri = data.getData();
				if (data.getBooleanExtra(HandriteIntent.EXTRA_DELETE, false)) {
					noteImage.setImageDrawable(null);
					try {
						File noteFile = new File(new URI(noteUri.toString()));
						Boolean deleted = noteFile.delete();
						EXPORT_FILE.delete();
						Toast.makeText(this,
								deleted ? "deleted" : "delete failed",
								Toast.LENGTH_SHORT).show();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				} else {
					noteImage.setImageDrawable(null);
					noteImage.setImageURI((Uri) data
							.getParcelableExtra(HandriteIntent.EXTRA_EXPORT));
					textViewLabel.setText(data
							.getStringExtra(HandriteIntent.EXTRA_LABELS));
					textViewAppendText.setText(data
							.getCharSequenceExtra(HandriteIntent.EXTRA_TEXT));
				}
			}
			break;
		case REQUEST_CODE_SELECT_LOCAL_PICTURE:
			if (resultCode == RESULT_OK) {
				textViewAppendImageUri.setText(data.getDataString());
			}
			if (!isValidLocalImageUriForAppend()) {
				checkBoxAppendImage.setChecked(false);
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private static File imageUriToFile(Context context, Uri uri)
			throws URISyntaxException {
		String scheme = uri.getScheme();
		if (scheme != null && scheme.equalsIgnoreCase("file")) {
			return new File(new URI(uri.toString()));
		} else if (scheme != null && scheme.equalsIgnoreCase("content")) {
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = context.getContentResolver().query(uri,
					filePathColumn, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				String filePath = cursor.getString(columnIndex);
				cursor.close();
				if (filePath != null) {
					return new File(filePath);
				}
			}
			return null;
		} else {
			throw new URISyntaxException(uri.toString(),
					"only support file:// and content://");
		}
	}
}
