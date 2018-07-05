import gui.JRangeSlider;
import gui.JTFSlider;
import gui.JTFSliderDouble;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jgridmaker.GMPanel;
import main.ImageStack;
import main.MasterControl;
import main.Segment;
import main.seggen.connect.ConnectedPart; //!
import main.seggen.connect.ConnectedPartsFinder; //!
import main.tools.ToolSegGen;
import misc.AxisAlignedBoundingBox; //!
import misc.Voxel;
import misc.grid.BitCube;
import misc.messages.Message;
import misc.messages.YObservable;
import misc.messages.YObserver;
import settings.SettingsOwner;
import threads.SegmentingThread;
import yplugins.YModule;


/**
 * This class contains the GUI elements and their functionalities when triggered.
 * @author Chi Nguyen and Alexander Stojkovic.
 */


public class SampleSegModule extends GMPanel implements YModule, YObserver {
	
	public ArrayList<ArrayList<ConnectedPart>> connections2D = new ArrayList<ArrayList<ConnectedPart>>();
	public int test_number, slide_number, SEG_MAX;
	public int min_range, current_min = 0, max_range, current_max = 1;	
	public final int SEG_MIN = 0;
	public ArrayList<CriticalPoint> cpArray = new ArrayList<CriticalPoint>();
	public ArrayList<ArrayList<ConnectedPart>> segments = new ArrayList<ArrayList<ConnectedPart>>();
	public ArrayList<ReebGraph> rg_Array = new ArrayList<ReebGraph>();
	
	//Analyze the model
	private class SampleSegThread extends SegmentingThread {
		public SampleSegThread(Segment seg, SettingsOwner parent, boolean monitor) {
			super(seg, parent, monitor);
		}
				
public void my_run() {
			
			cpArray.clear();
			segments.clear();
			rg_Array.clear();
			
			BitCube bc = _seg.get_bc();
			
			int dim_z = bc.get_dim_z();
			int dim_y = bc.get_dim_y();
			int dim_x = bc.get_dim_x();

			
			for(int z = 0; z < dim_z; z++) {

				//legt die Ebene fest
				AxisAlignedBoundingBox aabb = new AxisAlignedBoundingBox(0, 0, z, dim_x-1, dim_y-1, z); 
				ConnectedPartsFinder cpf = new ConnectedPartsFinder(bc, aabb);	  //ConnectedPartsFinder mit BoundingBox
				ArrayList<ConnectedPart> parts2D = cpf.detect_connected_parts(null); 

				
				if(!parts2D.isEmpty()) {
					
					for(int i = 0; i < parts2D.size(); i++) {

						
						if(parts2D.get(i).get_voxel_number() > 15) {
							
							ReebGraph rg = new ReebGraph(bc, new CriticalPoint(parts2D.get(i)));	//öffnet neunen RG mit einem Kritischenpunkt
							rg.detect();	//führt die analyse durch
							
							ArrayList<ConnectedPart> temp2D = rg.getElements();		//gibt alle Elemente zurück
							
							if(temp2D != null && temp2D.size() > 2) {	
								segments.add(temp2D);
								bc = new BitCube(rg.getBc());
								rg_Array.add(rg);
							}
							else {
							parts2D.get(i).delete_from(bc);
							}
						}
					}
				}
				

			}
			
			bc.clear();

			_seg.new_data(true);
			System.out.println("finished " + rg_Array.size() + " ReebGraphen gefunden");

		}
	}
	
	//Draw the model in 3D 
		private class SampleSecondThread extends SegmentingThread {
			public SampleSecondThread(Segment seg, SettingsOwner parent, boolean monitor) {
				super(seg, parent, monitor);
			}

			public void showSegment() {

				if (slide_number >= rg_Array.size()) {
					System.out.println("Leerer Index bei slide_number: " + slide_number);
					return;
				}

				Random r = new Random();
				String seg_name = "ReebGraph";
				Segment seg = MasterControl.get_is().get_segment(seg_name);
				if (seg == null) seg = MasterControl.get_is().create_segment(seg_name, r.nextInt());
				seg.get_bc().clear();

				connections2D = rg_Array.get(slide_number).getConnections2D();
				
				for (int n = min_range; n < max_range && n < connections2D.size(); n++) {
					for (int i = 0; i < connections2D.get(n).size(); i++) {
						connections2D.get(n).get(i).add_to(seg.get_bc());
					}
				}
				seg.new_data(true);
				seg.notifyObservers();
			}

			public void my_run() {
				showSegment();
				return;
			}
		}
		

		public SampleSegModule() {
			
			final JSlider jb_slide_it = new JSlider(JSlider.HORIZONTAL,SEG_MIN,segments.size(),SEG_MIN);
			final JTextField jb_text_it = new JTextField(segments.size());
			final JRangeSlider range = new JRangeSlider(min_range, max_range, min_range, max_range, 1);
			final JTextField jb_maximum = new JTextField(connections2D.size());
			final JTextField jb_minimum = new JTextField("0", 1);
			final JTextField jb_current_min = new JTextField(min_range);
			final JTextField jb_current_max = new JTextField(max_range);
			
			//Range Slider
			range.addChangeListener(new ChangeListener() {
				public void stateChanged(final ChangeEvent e) {
					
					JRangeSlider source = (JRangeSlider)e.getSource();			
					min_range = source.getLowValue();
					max_range = source.getHighValue();
					jb_current_min.setText(new Integer(min_range).toString());
					jb_current_max.setText(new Integer(max_range).toString());
					
					System.out.print(min_range + " ");
					System.out.println(max_range);

					Segment tmp_seg = MasterControl.get_is().get_segment(ToolSegGen.TMP_SEG_NAME);
					SampleSecondThread my_thread = new SampleSecondThread(tmp_seg, null, true);
					my_thread.start();								
				}
			});		
			add("range", range);

			//Current min range field
			jb_current_min.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					current_min = Integer.parseInt(jb_current_min.getText());
					range.setLowValue(current_min);
					max_range = current_max;
				}
			});
			add("current_min", jb_current_min);
			
			//Current max range field
			jb_current_max.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					current_max = Integer.parseInt(jb_current_max.getText());
					range.setLowValue(current_max);
					max_range = current_max;
				}
			});
			add("current_max", jb_current_max);
			
			//BOUNDARY MIN FIELD TEXT
			jb_minimum.setEditable(false);
			add("minimum", jb_minimum);
			
			//BOUNDARY MAX FIELD TEXT
			jb_maximum.setEditable(false);
			add("maximum", jb_maximum);
			
			//Slider Seg chooser
			jb_slide_it.addChangeListener(new ChangeListener() {
				public void stateChanged(final ChangeEvent e) {
					
					JSlider source = (JSlider)e.getSource();
					
					if (!source.getValueIsAdjusting()) {
						slide_number = (int)source.getValue();
						System.out.println(slide_number);
						jb_text_it.setText(new Integer(slide_number).toString());
						connections2D = rg_Array.get(slide_number).getConnections2D();
						min_range = 0;
						int size = connections2D.size();
						
						range.setMinimum(min_range);
						range.setLowValue(min_range);
						range.setMaximum(size);
						if (size > 0) range.setHighValue(1);
						jb_current_min.setText(new Integer(min_range).toString());
						if (size > 0) jb_current_max.setText("1");
						jb_maximum.setText(new Integer(size).toString());
					}
					
					Segment tmp_seg = MasterControl.get_is().get_segment(ToolSegGen.TMP_SEG_NAME);
					SampleSecondThread my_thread = new SampleSecondThread(tmp_seg, null, true);
					my_thread.start();			
				}
			});	
			add("slide_it", jb_slide_it);
			
			//Max text field next to slider
	        jb_text_it.addActionListener(new ActionListener() {		
				public void actionPerformed(final ActionEvent e) {
					int size = connections2D.size();
					int seg_to_choose = Integer.parseInt(jb_text_it.getText());
					min_range = 0;
					
					range.setMinimum(min_range);
					range.setLowValue(min_range);
					range.setMaximum(size);
					if (size > 0) range.setHighValue(1);
					jb_current_min.setText(new Integer(min_range).toString());
					if (size > 0) jb_current_max.setText("1");
					jb_maximum.setText(new Integer(size).toString());
					jb_slide_it.setValue(seg_to_choose);			
				}
			});
			add("text_it", jb_text_it);
					
			//Analyze button
			final JButton jb_Analysis = new JButton("1. Analyze");
			jb_Analysis.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					
					Segment tmp_seg = MasterControl.get_is().get_segment(ToolSegGen.TMP_SEG_NAME);
					SampleSegThread my_thread = new SampleSegThread(tmp_seg, null, true);
					my_thread.start();				
				}
			});
			add("do_it", jb_Analysis);
				
			//Draw button
			final JButton jb_Draw = new JButton("2. Draw");
			jb_Draw.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					int size = segments.size() - 1;
					System.out.println("SEGMENTS = " + size);
					jb_slide_it.setMaximum(segments.size() - 1);
					String max = new Integer(segments.size() - 1).toString();
					jb_text_it.setText(max);
					jb_maximum.setText(new Integer(connections2D.size()).toString());
					range.setMaximum(connections2D.size());
					range.setHighValue(1);
					
					Segment tmp_seg = MasterControl.get_is().get_segment(ToolSegGen.TMP_SEG_NAME);
					SampleSecondThread my_thread = new SampleSecondThread(tmp_seg, null, true);
					my_thread.start();
				}
			});
			add("make_it", jb_Draw);
			
			set_layout(""+
				"<table>"+
				  "<tr>"+
				    "<td>::do_it::</td>"+
				    "<td> then </td>"+
				    "<td>::make_it::</td>"+
				  "</tr>"+
				    
				  "<tr>"+
					"<td> Choose segment: </td>"+
				    "<td>::slide_it::</td>"+
				    "<td>::text_it::</td>"+
				   "</tr>"+
				    
				   "<tr>"+
				   	"<td>::minimum::</td>"+
				    "<td>::range::</td>"+
				    "<td>::maximum::</td>"+
				   "</tr>"+
				    
				   "<tr>"+
				   "<td>::current_min::</td>"+
				   "<td> Choose range in segments </td>"+
				   "<td>::current_max::</td>"+
				   "</tr>"+
				"</table>");				

			ImageStack is = MasterControl.get_is();
			is.addObserver(this, "Sample Module Listener");
		}
		
		@Override
		public String get_module_name() {
			return "ProPra";
		}

		@Override
		public String get_module_short_descr() {
			return "A small sample Module to learn YPlugin programming";
		}

		@Override
		public JPanel get_module_interface() {
			return this;
		}

		@Override
		public void update(YObservable sender, Message m) {
			System.out.println("SampleSegModule::update received message from "+sender.getClass()+": "+Message.get_message_string(m._type));
			
			if (sender.getClass()==ImageStack.class) {
				if (m._type == ImageStack.M_LOADING_END) {
					// we good
				}
			}
		}

	}
