

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import main.ImageStack;
import main.MasterControl;
import main.MenuBar;
import misc.dicom.DiFile;
import misc.dicom.DiFileInputStream;
import misc.messages.Message;
import misc.messages.YObservable;
import misc.messages.YObserver;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import yplugins.YModuleType;
import yplugins.YPlugin;

@PluginImplementation
public class ProPraPlugin implements YPlugin,YObserver {
	@Init
	public void init() {
		MasterControl.register_module(new SampleSegModule(), YModuleType.SEGMENTING);		
		MasterControl.get_is().addObserver(this, "ProPraPlugin wants to know when GUI is ready");
		
		System.out.println(get_plugin_name()+" initialized!!!.");
	}
	
	@Override
	public String get_plugin_name() {
		return "ProPra 2015 Plugin";
	}

	@Override
	public String get_plugin_short_descr() {
		return "Plugin to learn how to build a Plugin";
	}

	@Override
	public void update(YObservable sender, Message m) {
		System.out.println("ProPraPlugin::update received message from "+sender.getClass()+": "+Message.get_message_string(m._type));

		JMenuItem item = new JMenuItem(new String("DICOM Browser"));
		item.addActionListener(new ActionListener() {
			@Override
	        public void actionPerformed(final ActionEvent event) {
				System.out.println("Here could be the code for opening the DICOM library");

				// sample code
				DiFile df = new DiFile();
				try {
					String file_name = "resources/testdata/CTHd001";
					
					DiFileInputStream dfis = new DiFileInputStream(file_name);
					boolean valid_dicom = dfis.skip_header();
					dfis.close();
					
					if (valid_dicom) {
						// open a DICOM file
						df.load_from_file("resources/testdata/CTHd001");
						
						// access a few data elements and print them to console
						String name = df.get_data_elements().get((0x00100010)).get_value_as_string();
						int rows = df.get_data_elements().get((0x00280010)).get_value_as_int();
						int cols = df.get_data_elements().get((0x00280011)).get_value_as_int();
						
						System.out.println("Patient Name: "+name);
						System.out.println("Image Dimension: "+rows+" x "+cols);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		if (m._type==ImageStack.M_INITIALIZED) {
			MasterControl.get_menu().add_entry(MenuBar.MENU_FILE, item, true, true);
		}
	}
}
