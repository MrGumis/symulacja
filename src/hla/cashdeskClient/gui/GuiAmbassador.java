package hla.cashdeskClient.gui;

import org.portico.impl.hla13.types.DoubleTime;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReflectedAttributes;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;
import hla13.Example13Federate;

public class GuiAmbassador extends NullFederateAmbassador{
	protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    protected boolean running 			 = true;
    
    protected int statisticsHandle;
    
   
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
    
    public void discoverObjectInstance( int theObject, int theObjectClass, String objectName )
	{
		log( "Discoverd Object: handle=" + theObject + ", classHandle=" +
		theObjectClass + ", name=" + objectName );
		statisticsHandle = theObject;
	}
    
    public void reflectAttributeValues( int theObject,
            ReflectedAttributes theAttributes,
            byte[] tag )
	{
	// just pass it on to the other method for printing purposes
	// passing null as the time will let the other method know it
	// it from us, not from the RTI
	reflectAttributeValues( theObject, theAttributes, tag, null, null );
	}
	
	public void reflectAttributeValues( int theObject,
	            ReflectedAttributes theAttributes,
	            byte[] tag,
	            LogicalTime theTime,
	            EventRetractionHandle retractionHandle )
	{
		StringBuilder builder = new StringBuilder( "Reflection for object:" );
		
		// print the handle
		builder.append( " handle=" + theObject );
		// print the tag
		builder.append( ", tag=" + EncodingHelpers.decodeString(tag) );
		// print the time (if we have it) we'll get null if we are just receiving
		// a forwarded call from the other reflect callback above
		if( theTime != null )
		{
		builder.append( ", time=" + convertTime(theTime) );
		}
		
		// print the attribute information
		builder.append( ", attributeCount=" + theAttributes.size() );
		builder.append( "\n" );
		
		try
		{
		// print the attibute handle
		builder.append( "\tattributeHandle=" );
		builder.append( theAttributes.getAttributeHandle(0) );
		// print the attribute value
		builder.append( ", srCzasValue=" );
		builder.append(
		EncodingHelpers.decodeFloat(theAttributes.getValue(0)) );
		builder.append( "\n" );
		builder.append( "\tattributeHandle=" );
		builder.append( theAttributes.getAttributeHandle(1) );
		// print the attribute value
		builder.append( ", srDlugoscValue=" );
		builder.append(
		EncodingHelpers.decodeFloat(theAttributes.getValue(1)) );
		builder.append( "\n" );
		builder.append( "\tattributeHandle=" );
		builder.append( theAttributes.getAttributeHandle(2) );
		// print the attribute value
		builder.append( ", NumerKolejki=" );
		builder.append(
		EncodingHelpers.decodeInt(theAttributes.getValue(2)) );
		builder.append( "\n" );
		builder.append( "\tattributeHandle=" );
		builder.append( theAttributes.getAttributeHandle(3) );
		// print the attribute value
		builder.append( ", dlugoscKolejkiValue=" );
		builder.append(
		EncodingHelpers.decodeInt(theAttributes.getValue(3)) );
		builder.append( "\n" );
		builder.append( "\tattributeHandle=" );
		builder.append( theAttributes.getAttributeHandle(4) );
		// print the attribute value
		builder.append( ", czasOczekiwaniaValue=" );
		builder.append(
		EncodingHelpers.decodeFloat(theAttributes.getValue(4)) );
		builder.append( "\n" );
		}
		catch( ArrayIndexOutOfBounds aioob )
		{
		// won't happen
		}
		
		log( builder.toString() );
	
	}
}
