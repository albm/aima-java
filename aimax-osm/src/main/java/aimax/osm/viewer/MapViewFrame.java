package aimax.osm.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import aimax.osm.data.MapDataEvent;
import aimax.osm.data.MapDataEventListener;
import aimax.osm.data.MapDataStore;
import aimax.osm.data.Position;
import aimax.osm.data.entities.MapNode;
import aimax.osm.data.entities.MapWay;
import aimax.osm.data.entities.Track;
import aimax.osm.data.entities.MapNode.WayRef;
import aimax.osm.reader.MapReader;

/**
 * Implements a simple frame with a toolbar and a map view. The
 * toolbar provides buttons for map selection and navigation. Additionally,
 * a text field is included which shows informations like positions,
 * track length, and POI names. 
 * @author R. Lunde
 *
 */
public class MapViewFrame extends JFrame implements ActionListener {
	protected MapViewPane view;
	protected MapDataStore mapData;
	protected MapReader mapReader;
	protected JToolBar toolbar;
	
	private JFileChooser fileChooser;
	private JButton loadButton;
	private JButton findButton;
	private JTextField findField;
	protected JTextField infoField;
	
	public MapViewFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fileChooser = new JFileChooser();
		toolbar = new JToolBar();
		loadButton = new JButton("Load");
		loadButton.addActionListener(this);
		toolbar.add(loadButton);
		toolbar.addSeparator();
		findField = new JTextField();
		toolbar.add(findField);
		findField.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER)
					find(findField.getText());
			}
			public void keyReleased(KeyEvent e) {}
			public void keyTyped(KeyEvent e) {}});
		findButton = new JButton("Find");
		findButton.addActionListener(this);
		toolbar.add(findButton);
		toolbar.addSeparator();
		infoField = new JTextField(20);
		infoField.setEditable(false);
		toolbar.add(infoField);
		getContentPane().add(toolbar, BorderLayout.NORTH);
		
		mapData = new MapDataStore();
		view = new MapViewPane();
		view.setModel(mapData);
		getContentPane().add(view, BorderLayout.CENTER);
		MapEventHandler eventHandler = new MapEventHandler();
		mapData.addMapDataEventListener(eventHandler);
		view.addMapViewEventListener(eventHandler);
	}
		
	public MapViewFrame(MapReader mapReader, InputStream defaultMap) {
		this();
		this.mapReader = mapReader;
		if (mapReader != null) {
			FileFilter filter = new FileNameExtensionFilter
			(mapReader.fileFormatDescription(), mapReader.fileFormatExtension());
			fileChooser.addChoosableFileFilter(filter);
			if (defaultMap != null)
				mapReader.readMap(defaultMap, mapData);
		}
	}
	
	public void setVisible(boolean b) {
		super.setVisible(b);
		if (b && !mapData.isEmpty())
			view.adjustToFit();
	}
	
	public MapViewFrame(MapReader mapReader, File defaultMap) {
		this(mapReader, createStream(defaultMap));
		if (defaultMap != null) {
			fileChooser.setSelectedFile(defaultMap.getAbsoluteFile());			
		}
	}
	
	private static FileInputStream createStream(File file) {
		FileInputStream result = null;
		try {
			result = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// just return null...
		}
		return result;
	}
	
	public MapDataStore getMapData() {
		return mapData;
	}
	
	public MapViewPane getView() {
		return view;
	}
	
	public JToolBar getToolbar() {
		return toolbar;
	}
	
	public void find(String namepart) {
		Position pos = null;
		List<MapNode> marks = mapData.getMarks();
		if (!marks.isEmpty())
			pos = new Position(marks.get(mapData.getMarks().size()-1));
		else
			pos = view.getCenterPosition();
		MapNode node = mapData.findNode
		(findField.getText(), pos, true, true, true);
		if (node != null) {
			mapData.addMark(node.getLat(), node.getLon());
			view.adjustToCenter(node.getLat(), node.getLon());
			String name = node.getName();
			if (name == null) {
				name = "";
				for (WayRef wr : node.getWays()) {
					MapWay way = mapData.getWay(wr.wayId);
					String wayName = way.getName();
					if (wayName != null && !name.contains(wayName)) {
						if (!name.isEmpty())
							name = name + " / ";
						name = name + way.getName();
					}
				}
			}
			infoField.setText("Found: " + name);
		} else {
			infoField.setText("");
		}
	}
	
	/**
	 * Defines what happens when a button is pressed. 
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == loadButton) {
			if (mapReader != null) {
				int returnVal = fileChooser.showOpenDialog(this);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	mapReader.readMap(fileChooser.getSelectedFile(), mapData);
			    }
			}    
		} else if (e.getSource() == findButton) {
			find(findField.getText());
		}
	}
	
	
	/**
	 * Updates the info field based on events sent by the MapViewPane. 
	 * @author R. Lunde
	 */
	class MapEventHandler implements MapDataEventListener, MapViewEventListener {

		@Override
		public void eventHappened(MapDataEvent event) {
			if (event.getType() == MapDataEvent.Type.MAP_NEW) {
				infoField.setText("Ways: " + mapData.getWays().size() + ", Way Nodes: "
						+ mapData.getWayNodes().size() + ", POIs: " + mapData.getPOIs().size());
			} else if (event.getType() == MapDataEvent.Type.MARK_ADDED) {
				List<MapNode> nodes = mapData.getMarks();
				DecimalFormat f1 = new DecimalFormat("#0.00");
				MapNode mark = nodes.get(nodes.size()-1);
				infoField.setText("Mark " + mark.getName() + ": Lat "
						+ f1.format(mark.getLat()) + "; Lon "
						+ f1.format(mark.getLon()));
			} else if (event.getType() == MapDataEvent.Type.TRACK_MODIFIED) {
				Track track = mapData.getTrack(event.getObjId());
				if (track != null) {
					List<MapNode> nodes = track.getTrkPts();
					DecimalFormat f1 = new DecimalFormat("#0.00");
					double km = Position.getTrackLengthKM(nodes);
					String info = track.getName() + ": Length " + f1.format(km) + " km";
					if (nodes.size() > 1) {
						DecimalFormat f2 = new DecimalFormat("#000");
						MapNode m1 = nodes.get(nodes.size() - 2);
						MapNode m2 = nodes.get(nodes.size() - 1);
						int course = new Position(m1).getCourseTo(m2);
						info += "; Course " + f2.format(course);
					}
					infoField.setText(info);
				}
			} else {
				infoField.setText("");
			}
			
		}

		@Override
		public void eventHappened(MapViewEvent event) {
			if (event.getType() == MapViewEvent.Type.ZOOM) {
				if (mapData.getMarks().isEmpty())
					infoField.setText("Scale: " + (int) view.getTransformer().getScale());
			}
		}
	}
}