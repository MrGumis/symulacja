package hla.cashdeskClient.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Random;

import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import hla.rti.AttributeHandleSet;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.SuppliedAttributes;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;


public class ClientFederate {
	public static final String READY_TO_RUN = "ReadyToRun";
	
	private RTIambassador rtiamb;
    private ClientAmbassador fedamb;
    
    protected int numberQueue;
	
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
		 
		 fedamb = new ClientAmbassador(this);
	     rtiamb.joinFederationExecution( "ClientFederate", "CashdeskCilentFederation", fedamb );
	     log( "Joined Federation as ClientFederate");

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
	     
	     while (fedamb.running) {
	            advanceTime(randomTime());
	            publishClient();
	        }
	}
	
	 private void log( String message ){
		 System.out.println( "ClientFederate   : " + message );
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
		int klientHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Klient");
        int nrKolejkiHandle    = rtiamb.getAttributeHandle( "nrKolejki", klientHandle );
        int liczbaProduktowHandle = rtiamb.getAttributeHandle("liczbaProduktow", klientHandle);

        AttributeHandleSet attributes =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes.add( nrKolejkiHandle );
        attributes.add(liczbaProduktowHandle);
        
        rtiamb.publishObjectClass(klientHandle, attributes);
        
        int najkrotszaKolejkaHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.najkrotszaKolejka" );
        fedamb.najkrotszaKolejkaHandle = najkrotszaKolejkaHandle;
        rtiamb.subscribeInteractionClass(najkrotszaKolejkaHandle);
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
	
	private double randomTime(){
        Random r = new Random();
        return (double)(300 + r.nextInt(10));
    }
	
	private int randomProductNumber(){
		Random r = new Random();
		return 1 + r.nextInt(9);
	}
	
	private void publishClient() throws RTIexception{
		int klientClassHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Klient");
        int klientHandle = rtiamb.registerObjectInstance(klientClassHandle);
        
        int nrKolejkiHandle    = rtiamb.getAttributeHandle( "nrKolejki", klientClassHandle );
        int liczbaProduktowHandle = rtiamb.getAttributeHandle("liczbaProduktow", klientClassHandle);
        
         
        SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
        
        byte[] nrKolejkiValue = EncodingHelpers.encodeInt(numberQueue);
        byte[] liczbaProduktowValue = EncodingHelpers.encodeInt(randomProductNumber());
        
        attributes.add(nrKolejkiHandle, nrKolejkiValue);
        attributes.add(liczbaProduktowHandle, liczbaProduktowValue);
        
        rtiamb.updateAttributeValues( klientHandle, attributes, "klient attributes".getBytes());
   
	}
	
	public static void main( String[] args ){
		try{
			new ClientFederate().runFederate();
		}
		catch( RTIexception rtie ){
			rtie.printStackTrace();
		}
	}
}
