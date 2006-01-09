/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.common.codec.compare;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import javax.naming.Name;

import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.tlv.Length;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.asn1.ber.tlv.Value;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.util.LdapString;
import org.apache.ldap.common.name.LdapDN;
import org.apache.ldap.common.util.StringTools;


/**
 * A CompareRequest Message. Its syntax is :
 * CompareRequest ::= [APPLICATION 14] SEQUENCE {
 *              entry           LDAPDN,
 *              ava             AttributeValueAssertion }
 * 
 * AttributeValueAssertion ::= SEQUENCE {
 *              attributeDesc   AttributeDescription,
 *              assertionValue  AssertionValue }
 * 
 * AttributeDescription ::= LDAPString
 * 
 * AssertionValue ::= OCTET STRING
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CompareRequest extends LdapMessage
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The entry to be compared */
    private Name entry;

    /** The attribute to be compared */
    private LdapString attributeDesc;

    /** The value to be compared */
    private Object assertionValue;

    /** The compare request length */
    private transient int compareRequestLength;

    /** The attribute value assertion length */
    private transient int avaLength;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new CompareRequest object.
     */
    public CompareRequest()
    {
        super( );
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the message type
     *
     * @return Returns the type.
     */
    public int getMessageType()
    {
        return LdapConstants.COMPARE_REQUEST;
    }

    /**
     * Get the entry to be compared
     *
     * @return Returns the entry.
     */
    public String getEntry()
    {
        return ( ( entry == null ) ? "" : entry.toString() );
    }

    /**
     * Set the entry to be compared
     *
     * @param entry The entry to set.
     */
    public void setEntry( Name entry )
    {
        this.entry = entry;
    }

    /**
     * Get the assertion value
     *
     * @return Returns the assertionValue.
     */
    public Object getAssertionValue()
    {
        return assertionValue;
    }

    /**
     * Set the assertion value
     *
     * @param assertionValue The assertionValue to set.
     */
    public void setAssertionValue( Object assertionValue )
    {
        this.assertionValue = assertionValue;
    }

    /**
     * Get the attribute description
     *
     * @return Returns the attributeDesc.
     */
    public String getAttributeDesc()
    {
        return ( ( attributeDesc == null ) ? "" : attributeDesc.getString() );
    }

    /**
     * Set the attribute description
     *
     * @param attributeDesc The attributeDesc to set.
     */
    public void setAttributeDesc( LdapString attributeDesc )
    {
        this.attributeDesc = attributeDesc;
    }

    /**
     * Compute the CompareRequest length
     * 
     * CompareRequest :
     * 
     * 0x6E L1 
     *  |
     *  +--> 0x04 L2 entry
     *  +--> 0x30 L3 (ava)
     *        |
     *        +--> 0x04 L4 attributeDesc
     *        +--> 0x04 L5 assertionValue
     * 
     * L3 = Length(0x04) + Length(L4) + L4
     *      + Length(0x04) + Length(L5) + L5
     * 
     * Length(CompareRequest) = Length(0x6E) + Length(L1) + L1
     *                          + Length(0x04) + Length(L2) + L2
     *                          + Length(0x30) + Length(L3) + L3
     * @return DOCUMENT ME!
    */
    public int computeLength()
    {

        // The entry
        compareRequestLength = 1 + Length.getNbBytes( LdapDN.getNbBytes( entry ) ) + LdapDN.getNbBytes( entry );

        // The attribute value assertion
        avaLength =
            1 + Length.getNbBytes( attributeDesc.getNbBytes() ) + attributeDesc.getNbBytes();

        if ( assertionValue instanceof String )
        {
            int assertionValueLength = StringTools.getBytesUtf8( (String)assertionValue ).length;
            avaLength +=
                1 + Length.getNbBytes( assertionValueLength ) + assertionValueLength;
        }
        else
        {
            avaLength +=
                1 + Length.getNbBytes( ((byte[])assertionValue).length ) + ((byte[])assertionValue).length;
        }

        compareRequestLength += 1 + Length.getNbBytes( avaLength ) + avaLength;

        return 1 + Length.getNbBytes( compareRequestLength ) + compareRequestLength;
    }

    /**
     * Encode the CompareRequest message to a PDU.
     * 
     * CompareRequest :
     * 
     * 0x6E LL
     *   0x04 LL entry
     *   0x30 LL attributeValueAssertion
     *     0x04 LL attributeDesc
     *     0x04 LL assertionValue
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The CompareRequest Tag
            buffer.put( LdapConstants.COMPARE_REQUEST_TAG );
            buffer.put( Length.getBytes( compareRequestLength ) ) ;

            // The entry
            Value.encode( buffer, LdapDN.getBytes( entry ) );

            // The attributeValueAssertion sequence Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( Length.getBytes( avaLength ) ) ;
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException("The PDU buffer size is too small !");
        }

        // The attributeDesc
        Value.encode( buffer, attributeDesc.getString() );

        // The assertionValue
        if ( assertionValue instanceof String )
        {
            Value.encode( buffer, (String)assertionValue );
        }
        else
        {
            Value.encode( buffer, (byte[])assertionValue );
        }

        return buffer;
    }

    /**
     * Get a String representation of a Compare Request
     *
     * @return A Compare Request String 
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Compare request\n" );
        sb.append( "        Entry : '" ).append( entry.toString() ).append( "'\n" );
        sb.append( "        Attribute description : '" ).append( attributeDesc.toString() ).append(
            "'\n" );
        sb.append( "        Attribute value : '" ).append( assertionValue.toString() ).append(
            "'\n" );

        return sb.toString();
    }
}
