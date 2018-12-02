package hla.cashdeskClient.queue;

import objects.Klient;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.LinkedList;

import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import hla.rti.AttributeHandleSet;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

public class QueueFederate {
public static final String READY_TO_RUN = "ReadyToRun";
	
	private RTIambassador rtiamb;
    private QueueAmbassador fedamb;
    
    private int queues = 2;
    LinkedList<LinkedList<Klient>> queueList = new LinkedList<LinkedList <Klient>>();
    LinkedList<Klient> clientList = new LinkedList<Klient>(); 
    LinkedList<Boolean> cashdeskEmptyList = new LinkedList<Boolean>();
	
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
		 
		 fedamb = new QueueAmbassador(this);
	     rtiamb.joinFederationExecution( "QueueFederate", "CashdeskCilentFederation", fedamb );
	     log( "Joined Federation as QueueFederate");

	     rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );

	     while( fedamb.isAnnounced == false ){
	            rtiamb.tick();
	     }
	     
	     for(int i=0;i<queues;i++){
	    	 queueList.add(new LinkedList<Klient>());
	    	 cashdeskEmptyList.add(true);
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
	            if(clientList.size()>0) clientToQueue();
	            sendToCashdesk();
	            sendInteractionNajkrotszaKolejka(fedamb.federateTime + fedamb.federateLookahead);
	            sendQueueLenght();
	            advanceTime(1.0);
	        }
	}
	
	 private void sendQueueLenght() throws RTIexception{
		 for(int i=0;i<queues;i++){
	    	 sendInteractionDlugoscKolejki(fedamb.federateTime + fedamb.federateLookahead, i+1, queueList.get(i).size());
	     }
		
	}

	private void sendInteractionDlugoscKolejki(double timeStep, int queue, int lenght) throws RTIexception {
		SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        byte[] nrKolejki = EncodingHelpers.encodeInt(queue);
        byte[] liczbaOsob = EncodingHelpers.encodeInt(lenght);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.dlugoscKolejki");
        int nrKolejkiHandle = rtiamb.getParameterHandle( "numerKolejki", interactionHandle );
        int liczbaOsobHandle = rtiamb.getParameterHandle( "liczbaOsob", interactionHandle );

        parameters.add(nrKolejkiHandle, nrKolejki);
        parameters.add(liczbaOsobHandle, liczbaOsob);

        LogicalTime time = convertTime( timeStep );
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
		
	}

	private void sendToCashdesk() throws RTIexception {
		for(int i=0;i<queues;i++){
			if(cashdeskEmptyList.get(i) == true && queueList.get(i).size() > 0){
				cashdeskEmptyList.set(i, false);
				sendInteractionObslugaKlienta(fedamb.federateTime + fedamb.federateLookahead, queueList.get(i).getFirst());
				sendInteractionCzasOczekiwania(fedamb.federateTime + fedamb.federateLookahead, i+1, fedamb.federateTime - queueList.get(i).getFirst().time);
				queueList.get(i).removeFirst();
			}
		}
	}

	private void sendInteractionCzasOczekiwania(double timeStep, int queue, double timeWaiting) throws RTIexception {
		SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        byte[] nrKolejki = EncodingHelpers.encodeInt(queue);
        byte[] czas = EncodingHelpers.encodeFloat((float)timeWaiting);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.czasOczekiwania");
        int nrKolejkiHandle = rtiamb.getParameterHandle( "numerKolejki", interactionHandle );
        int czasHandle = rtiamb.getParameterHandle( "czas", interactionHandle );

        parameters.add(nrKolejkiHandle, nrKolejki);
        parameters.add(czasHandle, czas);

        LogicalTime time = convertTime( timeStep );
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
		
	}

	private void sendInteractionObslugaKlienta(double timeStep, Klient client) throws RTIexception {
		  SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
	        byte[] nrKolejki = EncodingHelpers.encodeInt(client.numerKolejki);
	        byte[] liczbaProduktow = EncodingHelpers.encodeInt(client.liczbaProduktow);

	        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.obslugaKlienta");
	        int nrKolejkiHandle = rtiamb.getParameterHandle( "numerKolejki", interactionHandle );
	        int liczbaProduktowHandle = rtiamb.getParameterHandle( "liczbaProduktow", interactionHandle );

	        parameters.add(nrKolejkiHandle, nrKolejki);
	        parameters.add(liczbaProduktowHandle, liczbaProduktow);

	        LogicalTime time = convertTime( timeStep );
	        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
	}

	private void clientToQueue() {
		for(int i=0;i<clientList.size();i++){
			Klient client = clientList.getFirst();
			clientList.removeFirst();
			queueList.get(client.numerKolejki-1).add(client);
		}
	}

	private void log( String message ){
		 System.out.println( "QueueFederate   : " + message );
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
		int klientHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Klient");
		fedamb.klientClassHandle = klientHandle;
        int nrKolejkiHandle    = rtiamb.getAttributeHandle( "nrKolejki", klientHandle );
        int liczbaProduktowHandle = rtiamb.getAttributeHandle("liczbaProduktow", klientHandle);

        AttributeHandleSet attributes =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes.add( nrKolejkiHandle );
        attributes.add(liczbaProduktowHandle);
        
        rtiamb.subscribeObjectClassAttributes(klientHandle, attributes);
        
        int liczbaKasHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.liczbaKas" );
        fedamb.liczbaKasHandle = liczbaKasHandle;
        rtiamb.subscribeInteractionClass( liczbaKasHandle );
        
        int koniecObslugiHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.koniecObslugi" );
        fedamb.koniecObslugiHandle  = koniecObslugiHandle ;
        rtiamb.subscribeInteractionClass( koniecObslugiHandle  );
        
        //publish
        int obslugaKlientaHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.obslugaKlienta" );
        rtiamb.publishInteractionClass(obslugaKlientaHandle);
        
        int najkrotszaKolejkaHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.najkrotszaKolejka" );
        rtiamb.publishInteractionClass(najkrotszaKolejkaHandle);
        
        int dlugoscKolejkiHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.dlugoscKolejki" );
        rtiamb.publishInteractionClass(dlugoscKolejkiHandle);
        
        int czasOczekiwaniaHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.czasOczekiwania" );
        rtiamb.publishInteractionClass(czasOczekiwaniaHandle);
	 }
	
	private void sendInteractionNajkrotszaKolejka(double timeStep) throws RTIexception {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        byte[] nrKolejki = EncodingHelpers.encodeInt(checkShortestQueue());

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.najkrotszaKolejka");
        int nrKolejkiHandle = rtiamb.getParameterHandle( "numerKolejki", interactionHandle );

        parameters.add(nrKolejkiHandle, nrKolejki);

        LogicalTime time = convertTime( timeStep );
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
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
	
	private int checkShortestQueue(){
		int nr = 0;
		int size = queueList.get(0).size();
		for(int i=1;i<queues;i++){
			if(size>queueList.get(i).size()) nr = i;
		}
		return nr+1;
	}

	
	public static void main( String[] args ){
		try{
			new QueueFederate().runFederate();
		}
		catch( RTIexception rtie ){
			rtie.printStackTrace();
		}
	}
}
