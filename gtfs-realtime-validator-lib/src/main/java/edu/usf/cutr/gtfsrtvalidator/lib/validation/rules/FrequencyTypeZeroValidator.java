/*
 * Copyright (C) 2017 University of South Florida
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.usf.cutr.gtfsrtvalidator.lib.validation.rules;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

import edu.usf.cutr.gtfsrtvalidator.lib.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.util.RuleUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.*;

/**
 * Rules for frequency-based type 0 trips - trips defined in GTFS frequencies.txt with exact_times = 0
 *
 * E006 - Missing required vehicle_position trip field for frequency-based exact_times = 0
 * E013 - Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty
 * W005 - Missing vehicle_id in trip_update for frequency-based exact_times = 0
 */
public class FrequencyTypeZeroValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(FrequencyTypeZeroValidator.class);
    
    
    		
    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsMutableDao gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
        List<OccurrenceModel> errorListE006 = new ArrayList<>();
        List<OccurrenceModel> errorListE013 = new ArrayList<>();
        List<OccurrenceModel> errorListW005 = new ArrayList<>();
        List<OccurrenceModel> errorListE053 = new ArrayList<>();
        List<OccurrenceModel> errorListE054 = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
            	
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                        
                if (gtfsMetadata.getExactTimesZeroTripIds().contains(tripUpdate.getTrip().getTripId())) {
                    /**
                     * NOTE - W006 checks for missing trip_ids, because we can't check for that here - we need the trip_id to know if it's exact_times=0
                     */
                    if (!tripUpdate.getTrip().hasStartDate()) {
                        // E006 - Missing required trip_update trip field for frequency-based exact_times = 0
                        RuleUtils.addOccurrence(E006, "trip_id " + tripUpdate.getTrip().getTripId() + " is missing start_date", errorListE006, _log);
                    }

                    if (!tripUpdate.getTrip().hasStartTime()) {
                        // E006 - Missing required trip_update trip field for frequency-based exact_times = 0
                        RuleUtils.addOccurrence(E006, "trip_id " + tripUpdate.getTrip().getTripId() + " is missing start_time", errorListE006, _log);
                    }

                    if (!(!tripUpdate.getTrip().hasScheduleRelationship() || tripUpdate.getTrip().getScheduleRelationship().equals(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED))) {
                        // E013 - Validate schedule_relationship is UNSCHEDULED or empty
                        RuleUtils.addOccurrence(E013, "trip_id " + tripUpdate.getTrip().getTripId() + " schedule_relationship " + tripUpdate.getTrip().getScheduleRelationship(), errorListE013, _log);
                    }

                    if (!tripUpdate.hasVehicle() || !tripUpdate.getVehicle().hasId()) {
                        // W005 - Missing vehicle_id in trip_update for frequency-based exact_times = 0
                        RuleUtils.addOccurrence(W005, "trip_id " + tripUpdate.getTrip().getTripId(), errorListW005, _log);
                    }
                    
                    if(previousFeedMessage!=null)
                    {
	                    for (GtfsRealtime.FeedEntity previousEntity : previousFeedMessage.getEntityList()) 
	                    {                                        
	                    	if (previousEntity.hasTripUpdate()) 
	                    	{	
	                    		if(tripUpdate.getVehicle()!=null && previousEntity.getTripUpdate().getVehicle()!=null)
	                    		{                    				                    		
		                    		if(previousEntity.getTripUpdate().getVehicle().getId().equals(tripUpdate.getVehicle().getId()))
		                    		{				                               
					                    if(tripUpdate.getStopTimeUpdateCount()>0 && previousEntity.getTripUpdate().getStopTimeUpdateCount()>0)
					                    {				                    						                    		
					                    	if(tripUpdate.getStopTimeUpdate(tripUpdate.getStopTimeUpdateCount()-1).getStopSequence()>=previousEntity.getTripUpdate().getStopTimeUpdate(previousEntity.getTripUpdate().getStopTimeUpdateCount()-1).getStopSequence())
					                    	{
							                    // E053 - start time of trip not consistent for trip updates for frequency-based exact_times = 0
							                    if(!tripUpdate.getTrip().getStartTime().equals(previousEntity.getTripUpdate().getTrip().getStartTime()))
							                    {
							                    		RuleUtils.addOccurrence(E053, "vehicle_id " + tripUpdate.getVehicle().getId(),  errorListE053, _log);
							                    }		                    			                    	
					                    	}				                    						                  
					                    }	                    		
		                    		}
	                    		}
	                    	}
	                    }
                    }
                    
                    for(StopTimeUpdate stopTimeUpdate:tripUpdate.getStopTimeUpdateList())
                    {                    	
						if(stopTimeUpdate.hasArrival())
                    	{
                    		if(stopTimeUpdate.getArrival().hasDelay())
                    		{                    			
                    			RuleUtils.addOccurrence(E054, "vehicle_id " + tripUpdate.getVehicle().getId(),  errorListE054, _log);
                    		}
                    	}
                    	if(stopTimeUpdate.hasDeparture())
                    	{
                    		if(stopTimeUpdate.getDeparture().hasDelay())
                    		{                    		
                    			RuleUtils.addOccurrence(E054, "vehicle_id " + tripUpdate.getVehicle().getId(),  errorListE054, _log);
                    		}
                    	}
                    }
                }
            }
            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition vehiclePosition = entity.getVehicle();
                if (vehiclePosition.hasTrip() &&
                        gtfsMetadata.getExactTimesZeroTripIds().contains(vehiclePosition.getTrip().getTripId())) {

                    /**
                     * NOTE - W006 checks for missing trip_ids, because we can't check for that here - we need the trip_id to know if it's exact_times=0
                     */
                    if (!vehiclePosition.getTrip().hasStartDate()) {
                        // E006 - Missing required vehicle_position trip field for frequency-based exact_times = 0
                        RuleUtils.addOccurrence(E006, "vehicle_id " + vehiclePosition.getVehicle().getId() + " trip_id " + vehiclePosition.getTrip().getTripId() + " is missing start_date", errorListE006, _log);
                    }

                    if (!vehiclePosition.getTrip().hasStartTime()) {
                        // E006 - Missing required vehicle_position trip field for frequency-based exact_times = 0
                        RuleUtils.addOccurrence(E006, "vehicle_id " + vehiclePosition.getVehicle().getId() + " trip_id " + vehiclePosition.getTrip().getTripId() + " is missing start_time", errorListE006, _log);
                    }

                    if (!(!vehiclePosition.getTrip().hasScheduleRelationship() || vehiclePosition.getTrip().getScheduleRelationship().equals(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED))) {
                        // E013 - Validate schedule_relationship is UNSCHEDULED or empty
                        String prefix = "vehicle_id " + vehiclePosition.getVehicle().getId() + " trip_id " + vehiclePosition.getTrip().getTripId() + " schedule_relationship " + vehiclePosition.getTrip().getScheduleRelationship();
                        RuleUtils.addOccurrence(E013, prefix, errorListE013, _log);
                    }

                    if (!vehiclePosition.getVehicle().hasId()) {
                        // W005 - Missing vehicle_id for frequency-based exact_times = 0
                        RuleUtils.addOccurrence(W005, "entity ID" + entity.getId() + "with trip_id " + vehiclePosition.getTrip().getTripId(), errorListW005, _log);
                    }
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListE006.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E006), errorListE006));
        }
        if (!errorListE013.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E013), errorListE013));
        }
        if (!errorListW005.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W005), errorListW005));
        }
        if (!errorListE053.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E053), errorListE053));
        }        
        if (!errorListE054.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E054), errorListE053));
        }
        return errors;
    }
}
