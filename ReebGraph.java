import java.util.ArrayList;
import main.ImageStack;
import main.seggen.connect.ConnectedPart; //!
import main.seggen.connect.ConnectedPartsFinder; //!
import misc.AxisAlignedBoundingBox; //!
import misc.Voxel;
import misc.grid.BitCube;
import misc.messages.Message;
import misc.messages.YObservable;
import misc.messages.YObserver;



public class ReebGraph implements YObserver {
	
	private ArrayList<ArrayList<ConnectedPart>> connections2D;
	private ArrayList<CriticalPoint> cps;
	private int dim_z, cpNumber;
	private BitCube bc;
	private int[] dim_Min, dim_Max;
	
	
	
	
	ReebGraph(BitCube bc, CriticalPoint cp) {
		
		this.bc = new BitCube(bc);
		this.connections2D = new ArrayList<ArrayList<ConnectedPart>>();
		this.cps = new ArrayList<CriticalPoint>();
		this.cps.add(cp);
		this.dim_z = bc.get_dim_z();
		this.dim_Min = bc.get_bb_min();
		this.dim_Max = bc.get_bb_max();
		this.cpNumber = 0;
		
	}
	
	
	public ArrayList<ArrayList<ConnectedPart>> getConnections2D() {
		
		return connections2D;
	}
	
	public BitCube getBc() {
		
		return bc;
	}
	
	
	
	private int[][] getBoundings(Voxel v_Min, Voxel v_Max) {
		
		int[][] result = new int[2][3];
		int range = 50;
		
		result[0][0] = v_Min._x - range;
		result[0][1] = v_Min._y - range;
		result[0][2] = v_Min._z + 1;
		result[1][0] = v_Max._x + range;
		result[1][1] = v_Max._y + range;
		result[1][2] = v_Max._z + 1;
		
		if(result[0][0] < dim_Min[0]) result[0][0] = dim_Min[0];
		if(result[0][1] < dim_Min[1]) result[0][1] = dim_Min[1];
		if(result[1][0] > dim_Max[0]) result[1][0] = dim_Max[0];
		if(result[1][1] > dim_Max[0]) result[1][1] = dim_Max[1];
		
		
		return result;
	}
	
	
	
	private int getBiggest(ArrayList<ConnectedPart> parts2D ) { //findet größtes Element
			
		if(parts2D.isEmpty()) return -1;
		
		int biggest = 0;
		for(int i = 0; i < parts2D.size(); i++) {
				
			if(parts2D.get(i).get_voxel_number() > parts2D.get(biggest).get_voxel_number()) {
					
				biggest = i;
			}
		}	
		return biggest;			
	}
	
	
	
	private ArrayList<ConnectedPart> isCPBig(ArrayList<ConnectedPart> parts2D) { //untersucht die größe des Elements
		
		
		ArrayList<ConnectedPart> result = new ArrayList<ConnectedPart>();
		
		for (int i = 0; i < parts2D.size(); i++) {
			
			if (parts2D.get(i).get_voxel_number() > 20) result.add(parts2D.get(i)); 
			
		}
		
		return result;
	}
	
	
	
	private ArrayList<ConnectedPart> isInCPinLayer(ArrayList<ConnectedPart> parts2D, Voxel v_Min, Voxel v_Max) {
															
		
		ArrayList<ConnectedPart> result = new ArrayList<ConnectedPart>();  //Boundings mit Vorgänger vergleichen
		Voxel min, max;
		
		for (int i = 0; i < parts2D.size(); i++) {
			
			min = parts2D.get(i).get_bb_min();
			max = parts2D.get(i).get_bb_max();
			if ( v_Max._x > min._x && v_Max._y > min._y && max._x > v_Min._x && max._y > v_Min._y ) {
				result.add(parts2D.get(i));														
			}
		}
		
		return result;
	}
	
	
	
	private ArrayList<ConnectedPart> getBiggestPoints(ArrayList<ConnectedPart> parts2D) {
		

		ArrayList<ConnectedPart> result = new ArrayList<ConnectedPart>();	//gibt die Größten Elemente zurück
		
		int biggest = getBiggest(parts2D);
		int v_number = parts2D.get(biggest).get_voxel_number(); 
		
		for (int i = 0; i < parts2D.size(); i++) {
			
			if (Math.abs(parts2D.get(i).get_voxel_number() - v_number) < (v_number*2/5)) {
				
				 result.add(parts2D.get(i));
			}
		}
		
		return result;
	}
	
	
	
	public ArrayList<ConnectedPart> getElements() {		//gibt alle Gefundenen ConnectedParts in einen Array zurück
		
		
		ArrayList<ConnectedPart> result = new ArrayList<ConnectedPart>();
		
		for(int i = 0; i < connections2D.size(); i++) {
			
			result.addAll(connections2D.get(i));
		}
		
		System.out.println("ReebGraphsize: " + connections2D.size());
		return result;
		
	}
	
	
	
	public void detect() {			//analysiert den BitCube mit hilfe des CriticalPoint
		
		if(cps.isEmpty()) return;
		
		ArrayList<ConnectedPart> result;
		CriticalPoint cp;
		Voxel v_Min, v_Max;

		
		while(cpNumber < cps.size()) {
			
			result = new ArrayList<ConnectedPart>();
			cp = cps.get(cpNumber);
			v_Min = cp.getVMin();
			v_Max = cp.getVMax();
			result.add(cp.getCP());
			cp.getCP().delete_from(bc);

			
			for(int z = v_Min._z + 1; z < dim_z; z++) {
				
				//System.out.println(v_Min.toString() + "  " + v_Max.toString());
				int[][] boundings = getBoundings(v_Min, v_Max);
				AxisAlignedBoundingBox aabb = new AxisAlignedBoundingBox(boundings[0][0], boundings[0][1], 
						boundings[0][2], boundings[1][0], boundings[1][1], boundings[1][2]);
				ConnectedPartsFinder cpf = new ConnectedPartsFinder(bc, aabb);
				//System.out.println(boundings[1][2] + "  " + dim_z); //dim_z Abgleich
				ArrayList<ConnectedPart> temp = cpf.detect_connected_parts(null);
				
				if(temp.isEmpty()) break;
				
				temp = isCPBig(temp);
				if(temp.isEmpty()) break;

				temp = isInCPinLayer(temp, v_Min, v_Max);
				if(temp.isEmpty()) break;
				
				
				temp = getBiggestPoints(temp);
				if(temp.isEmpty()) break;
				
				
				
				
				if(temp.size() > 1) {
				
					for(int i = 0; i < temp.size(); i++) {
						
						cps.add(new CriticalPoint(temp.get(i)));
					}
					
					break;
				}
				
				result.add(temp.get(0));
				v_Min = temp.get(0).get_bb_min();
				v_Max = temp.get(0).get_bb_max();
				temp.get(0).delete_from(bc);
				
			}
			
			
			if(!result.isEmpty()) connections2D.add(result);
			cpNumber++;
			
		}
	}	
	
	
	
	public void update(YObservable sender, Message m) {
		System.out.println("SampleSegModule::update received message from "+sender.getClass()+": "+Message.get_message_string(m._type));
		
		if (sender.getClass()==ImageStack.class) {
			if (m._type == ImageStack.M_LOADING_END) {
				// do something with the new data
			}
		}
	}

}
