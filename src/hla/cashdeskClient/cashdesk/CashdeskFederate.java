package hla.cashdeskClient.cashdesk;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.LinkedList;

import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

public class CashdeskFederate {
public static final String READY_TO_RUN = "ReadyToRun";
	
	private RTIambassador rtiamb;
    private CashdeskAmbassador fedamb;
    
    private int queues = 2;
    LinkedList<Double> timeList = new LinkedList<Double>();
	
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
		 
		 fedamb = new CashdeskAmbassador(this);
	     rtiamb.joinFederationExecution( "CashdeskFederate", "CashdeskCilentFederation", fedamb );
	     log( "Joined Federation as CashdeskFederate");

	     rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );

	     while( fedamb.isAnnounced == false ){
	            rtiamb.tick();
	     }
	     
	     for(int i=0;i<queues;i++){
	    	 timeList.add(-1.0);
	     }
	     
	     waitForUser();

	     rtiamb.synchronizationPointAchieved( READY_TO_RUN );
	     log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
	     
	     while( fedamb.isReadyToRun == false ){
	    	 	rtiamb.tick();
	     }
	     
	     enableTimePolicy();

	     publishAndSubscribe();
	     
	     while (fedamb.running) {
	    	 	handlingClient();
	            advanceTime(1.0);
	        }
	}
	
	 private void handlingClient() throws RTIexception{
		 for(int i=0;i<queues;i++){
	    	 if(timeList.get(i) != -1.0){
	    		 if(timeList.get(i) <= fedamb.federateTime){
	    			 timeList.set(i, -1.0);
	    			 sendInteractionKoniecObslugi(fedamb.federateTime + fedamb.federateLookahead, i+1);
	    			 log("Oblsuzono klienta z kolejki: " + (i+1));
	    		 }
	    	 }
	     }
		
	}

	private void sendInteractionKoniecObslugi(double timeStep, int queue) throws RTIexception {
		SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        byte[] nrKolejki = EncodingHelpers.encodeInt(queue);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.koniecObslugi");
        int nrKolejkiHandle = rtiamb.getParameterHandle( "numerKolejki", interactionHandle );
        
        parameters.add(nrKolejkiHandle, nrKolejki);
        
        LogicalTime time = convertTime( timeStep );
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
	}

	private void log( String message ){
		 System.out.println( "CashdeskFederate   : " + message );
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
		int koniecObslugiHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.koniecObslugi" );
        rtiamb.publishInteractionClass(koniecObslugiHandle);
        
        int obslugaKlientaHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.obslugaKlienta" );
        fedamb.obslugaKlientaHandle= obslugaKlientaHandle;
        rtiamb.subscribeInteractionClass( obslugaKlientaHandle );
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
			new CashdeskFederate().runFederate();
		}
		catch( RTIexception rtie ){
			rtie.printStackTrace();
		}
	}
}
