package hla.cashdeskClient.statistics;

import org.portico.impl.hla13.types.DoubleTime;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;
import hla13.Example13Federate;

public class StatisticsAmbassador extends NullFederateAmbassador{
	protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    protected boolean running 			 = true;
    
    protected int liczbaKasHandle = 0;
    protected int koniecObslugiHandle = 0;
    protected int dlugoscKolejkiHandle = 0;
    protected int czasOczekiwaniaHandle = 0;
    
    protected StatisticsFederate fed;
    
    public StatisticsAmbassador(StatisticsFederate fed){
    	this.fed = fed;
    }
    

    private double convertTime( LogicalTime logicalTime )
    {
        // PORTICO SPECIFIC!!
        return ((DoubleTime)logicalTime).getTime();
    }

    private void log( String message )
    {
        System.out.println( "FederateAmbassador: " + message );
    }

    public void synchronizationPointRegistrationFailed( String label )
    {
        log( "Failed to register sync point: " + label );
    }

    public void synchronizationPointRegistrationSucceeded( String label )
    {
        log( "Successfully registered sync point: " + label );
    }

    public void announceSynchronizationPoint( String label, byte[] tag )
    {
        log( "Synchronization point announced: " + label );
        if( label.equals(Example13Federate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    public void federationSynchronized( String label )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(Example13Federate.READY_TO_RUN) )
            this.isReadyToRun = true;
    }

    /**
     * The RTI has informed us that time regulation is now enabled.
     */
    public void timeRegulationEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isConstrained = true;
    }

    public void timeAdvanceGrant( LogicalTime theTime )
    {
        this.federateTime = convertTime( theTime );
        this.isAdvancing = false;
    }
    
    public void receiveInteraction( int interactionClass,
            ReceivedInteraction theInteraction,
            byte[] tag )
	{
	// just pass it on to the other method for printing purposes
	// passing null as the time will let the other method know it
	// it from us, not from the RTI
	receiveInteraction(interactionClass, theInteraction, tag, null, null);
	}
	
	public void receiveInteraction( int interactionClass,
	            ReceivedInteraction theInteraction,
	            byte[] tag,
	            LogicalTime theTime,
	            EventRetractionHandle eventRetractionHandle )
	{
		StringBuilder builder = new StringBuilder( "Interaction Received:" );
		if(interactionClass == koniecObslugiHandle) {
			try {
			int numberQueue = EncodingHelpers.decodeInt(theInteraction.getValue(0));
			fed.summaryList.set(numberQueue-1, fed.summaryList.get(numberQueue-1)+1);
			builder.append(" KoniecObslugi , queue=" + numberQueue);
			} catch (ArrayIndexOutOfBounds ignored) {
			}
		}
		else{
			if(interactionClass == dlugoscKolejkiHandle){
				try {
					builder.append(" dlugosckolejki , queue=");
					int numberQueue = EncodingHelpers.decodeInt(theInteraction.getValue(0));
					int clientAmount = EncodingHelpers.decodeInt(theInteraction.getValue(1));
					fed.lengthUpdate(numberQueue, clientAmount);
				} catch (ArrayIndexOutOfBounds ignored) {
				}
			}else{
				if(interactionClass == czasOczekiwaniaHandle){
					try {
						builder.append(" czasoczewkiania , queue=");
						int numberQueue = EncodingHelpers.decodeInt(theInteraction.getValue(0));
						float time = EncodingHelpers.decodeFloat(theInteraction.getValue(1));
						fed.timeUpdate(numberQueue, time);
					} catch (ArrayIndexOutOfBounds ignored) {
					}
				}
			}
		}
		log( builder.toString() );
	}
}
