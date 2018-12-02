package objects;

import javax.swing.JFrame;

public class MainWindow extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2504120297829072756L;

	public MainWindow(){
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setTitle("Gui Federate");
		this.setSize(600, 300);
		
		
		this.setVisible(true);
	}
}
