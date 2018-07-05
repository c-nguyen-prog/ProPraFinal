import main.seggen.connect.ConnectedPart;
import misc.Voxel;


	/**
	 * 
	 * @author Chi Nguyen and Alexander Stojkovic.	
	 *  This class contains the data of a critical point, its coordinates and voxel min max
	 */
	public class CriticalPoint {

		private Voxel vmin, vmax;
		private ConnectedPart cp;


		
		/**
		 * Constructor
		 * @param vmin Bounding box voxel min
		 * @param vmax Bounding box voxel max
		 */
		public CriticalPoint(ConnectedPart cp) {

			this.cp = cp;
			vmin = cp.get_bb_min();
			vmax = cp.get_bb_max();
		}
		
		public CriticalPoint(Voxel vmin, Voxel vmax) {


			this.vmin = vmin;
			this.vmax = vmax;
		}
		
		
		public Voxel getVMin() {
			return vmin;
		}

		public Voxel getVMax() {
			return vmax;
		}
		
		public ConnectedPart getCP() {
			return cp;
		}
		
	}

