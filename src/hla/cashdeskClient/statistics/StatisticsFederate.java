package hla.cashdeskClient.statistics;

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
import hla.rti.SuppliedAttributes;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

public class StatisticsFederate {
public static final String READY_TO_RUN = "ReadyToRun";
	
	private RTIambassador rtiamb;
    private StatisticsAmbassador fedamb;
    private int queues = 2;
    LinkedList<Float> avgTimeList = new LinkedList<Float>();
    LinkedList<Float> avgLengthList = new LinkedList<Float>();
    LinkedList<Integer> lengthList = new LinkedList<Integer>();
    LinkedList<Float> timeWaitingList = new LinkedList<Float>();
    LinkedList<Integer> summaryList = new LinkedList<Integer>();
    LinkedList<Integer> handleList = new LinkedList<Integer>();
    private int countLenghtUpdate;
    private int countTimeUpdate;
    boolean update;
    
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
		 
		 fedamb = new StatisticsAmbassador(this);
	     rtiamb.joinFederationExecution( "StatisticsFederate", "CashdeskCilentFederation", fedamb );
	     log( "Joined Federation as StatisticsFederate");

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
	     
	     for(int i=0;i<queues;i++){
	    	 avgTimeList.add((float) 0);
	    	 avgLengthList.add((float) 0);
	    	 lengthList.add(0);
	    	 timeWaitingList.add((float) 0);
	    	 summaryList.add(0);
	    	 publishStatistics(avgTimeList.get(i),avgLengthList.get(i), i+1, lengthList.get(i), timeWaitingList.get(i));
	     }
	     countLenghtUpdate = 0;
	     countTimeUpdate = 0;
	     update = false;
	     
	     while (fedamb.running) {
	            if(update == true){
	            	updateStatistics();
	            	update=false;
	            }
	            advanceTime(1.0);
	        }
	}
	
	 private void updateStatistics() throws RTIexception{
		 int statystykiClassHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Statystyki");
		 for(int i=0;i<queues;i++){
			 int statystykiHandle = handleList.get(i);
			 int srCzasOczekiwaniaWKolejceHandle    = rtiamb.getAttributeHandle( "srCzasOczekiwaniaWKolejce", statystykiClassHandle );
			 int srDlugoscKolejkiHandle = rtiamb.getAttributeHandle("srDlugoscKolejki", statystykiClassHandle);
			 int nrKasyHandle    = rtiamb.getAttributeHandle( "nrKasy", statystykiClassHandle );
			 int dlugoscKolejkiHandle = rtiamb.getAttributeHandle("dlugoscKolejki", statystykiClassHandle);
			 int czasOczekiwaniaHandle = rtiamb.getAttributeHandle("czasOczekiwania", statystykiClassHandle);
			 
			 SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
			 byte[] srCzasOczekiwaniaWKolejceValue = EncodingHelpers.encodeFloat(avgTimeList.get(i));
			 byte[] srDlugoscKolejkiValue = EncodingHelpers.encodeFloat(avgLengthList.get(i));
			 byte[] nrKasyValue = EncodingHelpers.encodeInt(i+1);
			 byte[] dlugoscKolejkiValue = EncodingHelpers.encodeInt(lengthList.get(i));
			 byte[] czasOczekiwaniaValue = EncodingHelpers.encodeFloat(timeWaitingList.get(i));
			 
			 attributes.add(srCzasOczekiwaniaWKolejceHandle, srCzasOczekiwaniaWKolejceValue);
			 attributes.add(srDlugoscKolejkiHandle, srDlugoscKolejkiValue);
			 attributes.add(nrKasyHandle , nrKasyValue);
			 attributes.add(dlugoscKolejkiHandle, dlugoscKolejkiValue);
			 attributes.add(czasOczekiwaniaHandle, czasOczekiwaniaValue);
			 rtiamb.updateAttributeValues( statystykiHandle, attributes, "statystyki attributes".getBytes());
		 }
	}

	private void publishStatistics(Float avgTime, Float avgLength, int queue, Integer length, Float time) throws RTIexception{
		 int statystykiClassHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Statystyki");
		 int statystykiHandle = rtiamb.registerObjectInstance(statystykiClassHandle);
		 handleList.add(statystykiHandle);
		 int srCzasOczekiwaniaWKolejceHandle    = rtiamb.getAttributeHandle( "srCzasOczekiwaniaWKolejce", statystykiClassHandle );
		 int srDlugoscKolejkiHandle = rtiamb.getAttributeHandle("srDlugoscKolejki", statystykiClassHandle);
		 int nrKasyHandle    = rtiamb.getAttributeHandle( "nrKasy", statystykiClassHandle );
		 int dlugoscKolejkiHandle = rtiamb.getAttributeHandle("dlugoscKolejki", statystykiClassHandle);
		 int czasOczekiwaniaHandle = rtiamb.getAttributeHandle("czasOczekiwania", statystykiClassHandle);
		 
		 SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
		 byte[] srCzasOczekiwaniaWKolejceValue = EncodingHelpers.encodeFloat(avgTime);
		 byte[] srDlugoscKolejkiValue = EncodingHelpers.encodeFloat(avgLength);
		 byte[] nrKasyValue = EncodingHelpers.encodeInt(queue);
		 byte[] dlugoscKolejkiValue = EncodingHelpers.encodeInt(length);
		 byte[] czasOczekiwaniaValue = EncodingHelpers.encodeFloat(time);
		 
		 attributes.add(srCzasOczekiwaniaWKolejceHandle, srCzasOczekiwaniaWKolejceValue);
		 attributes.add(srDlugoscKolejkiHandle, srDlugoscKolejkiValue);
		 attributes.add(nrKasyHandle , nrKasyValue);
		 attributes.add(dlugoscKolejkiHandle, dlugoscKolejkiValue);
		 attributes.add(czasOczekiwaniaHandle, czasOczekiwaniaValue);
		 rtiamb.updateAttributeValues( statystykiHandle, attributes, "statystyki attributes".getBytes());
	}

	private void log( String message ){
		 System.out.println( "StatisticsFederate   : " + message );
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
		//publish
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
		
		rtiamb.publishObjectClass(statystykiHandle, attributes);
        
        //subscribe
        int liczbaKasHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.liczbaKas" );
        fedamb.liczbaKasHandle = liczbaKasHandle;
        rtiamb.subscribeInteractionClass(liczbaKasHandle);
        
        int koniecObslugiHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.koniecObslugi" );
        fedamb.koniecObslugiHandle = koniecObslugiHandle;
        rtiamb.subscribeInteractionClass(koniecObslugiHandle);
        
        dlugoscKolejkiHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.dlugoscKolejki" );
        fedamb.dlugoscKolejkiHandle = dlugoscKolejkiHandle;
        rtiamb.subscribeInteractionClass(dlugoscKolejkiHandle);
        
        czasOczekiwaniaHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.czasOczekiwania" );
        fedamb.czasOczekiwaniaHandle = czasOczekiwaniaHandle;
        rtiamb.subscribeInteractionClass(czasOczekiwaniaHandle);
	 }
	
	protected void lengthUpdate(int numberQueue, int clientAmount) {
		float temp = avgLengthList.get(numberQueue-1) * countLenghtUpdate;
		countLenghtUpdate++;
		lengthList.set(numberQueue-1, clientAmount);
		avgLengthList.set(numberQueue-1, (temp+clientAmount)/countLenghtUpdate);
		update = true;
	}

	protected void timeUpdate(int numberQueue, float time) {
		float temp = avgTimeList.get(numberQueue-1) * countTimeUpdate;
		countTimeUpdate++;
		timeWaitingList.set(numberQueue - 1, time);
		avgTimeList.set(numberQueue-1, (temp+time)/countTimeUpdate);
		update = true;
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
			new StatisticsFederate().runFederate();
		}
		catch( RTIexception rtie ){
			rtie.printStackTrace();
		}
	}

	
}
