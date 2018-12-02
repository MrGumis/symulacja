package hla.cashdeskClient.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import hla.rti.AttributeHandleSet;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.jlc.RtiFactoryFactory;
import objects.MainWindow;

public class GuiFederate {
public static final String READY_TO_RUN = "ReadyToRun";
	
	private RTIambassador rtiamb;
    private GuiAmbassador fedamb;
	
	public void runFederate() throws RTIexception{
		rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
		 try{
	            File fom = new File( "cashdeskclient.xml" );
	            rtiamb.createFederationExecution( "CashdeskCilentFederation",
	                    fom.toURI().toURL() );
	            log( "Created Federation" );
	     }
	     catch( FederationExecutionAlreadyExists exists ){
	            log( "Didn't create federation, it already existed" );
	     }
	     catch( MalformedURLException urle ){
	            log( "Exception processing fom: " + urle.getMessage() );
	            urle.printStackTrace();
	            return;
	     }
		 
		 fedamb = new GuiAmbassador();
	     rtiamb.joinFederationExecution( "GuiFederate", "CashdeskCilentFederation", fedamb );
	     log( "Joined Federation as GuiFederate");

	     rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );

	     while( fedamb.isAnnounced == false ){
	            rtiamb.tick();
	     }
	     
	     waitForUser();

	     rtiamb.synchronizationPointAchieved( READY_TO_RUN );
	     log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
	     
	     while( fedamb.isReadyToRun == false ){
	    	 	rtiamb.tick();
	     }
	     
	     enableTimePolicy();

	     publishAndSubscribe();
	     
	     @SuppressWarnings("unused")
		MainWindow window = new MainWindow();
	     
	     while (fedamb.running) {
	            advanceTime(1.0);
	        }
	}
	
	 private void log( String message ){
		 System.out.println( "GuiFederate   : " + message );
	 }
	 
	 private void waitForUser(){
	     log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
	     BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
	     try{
	         reader.readLine();
	     }
	     catch( Exception e ){
	         log( "Error while waiting for user input: " + e.getMessage() );
	         e.printStackTrace();
	     }
	 }
	 
	 private LogicalTime convertTime( double time ){
	        // PORTICO SPECIFIC!!
	     return new DoubleTime( time );
	 }

	    /**
	     * Same as for {@link #convertTime(double)}
	     */
	 private LogicalTimeInterval convertInterval( double time ){
	        // PORTICO SPECIFIC!!
	     return new DoubleTimeInterval( time );
	 }
	 
	 private void enableTimePolicy() throws RTIexception{
	     LogicalTime currentTime = convertTime( fedamb.federateTime );
	     LogicalTimeInterval lookahead = convertInterval( fedamb.federateLookahead );

	     this.rtiamb.enableTimeRegulation( currentTime, lookahead );

	     while( fedamb.isRegulating == false ){
	         rtiamb.tick();
	     }

	     this.rtiamb.enableTimeConstrained();

	     while( fedamb.isConstrained == false ){
	         rtiamb.tick();
	     }
	 }
	 
	private void publishAndSubscribe() throws RTIexception{
		//subscribe
		int statystykiHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Statystyki");
		int srCzasOczekiwaniaWKolejceHandle    = rtiamb.getAttributeHandle( "srCzasOczekiwaniaWKolejce", statystykiHandle );
		int srDlugoscKolejkiHandle = rtiamb.getAttributeHandle("srDlugoscKolejki", statystykiHandle);
		int nrKasyHandle    = rtiamb.getAttributeHandle( "nrKasy", statystykiHandle );
		int dlugoscKolejkiHandle = rtiamb.getAttributeHandle("dlugoscKolejki", statystykiHandle);
		int czasOczekiwaniaHandle = rtiamb.getAttributeHandle("czasOczekiwania", statystykiHandle);

		AttributeHandleSet attributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
		attributes.add(srCzasOczekiwaniaWKolejceHandle);
		attributes.add(srDlugoscKolejkiHandle);
		attributes.add(nrKasyHandle);
		attributes.add(dlugoscKolejkiHandle);
		attributes.add(czasOczekiwaniaHandle);
		
		rtiamb.subscribeObjectClassAttributes(statystykiHandle, attributes);
	 }
	
	private void advanceTime( double timestep ) throws RTIexception
    {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );
        while( fedamb.isAdvancing )
        {
            rtiamb.tick();
        }
    }
	
	public static void main( String[] args ){
		try{
			new GuiFederate().runFederate();
		}
		catch( RTIexception rtie ){
			rtie.printStackTrace();
		}
	}
}
