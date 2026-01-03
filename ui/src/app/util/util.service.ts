import { Injectable } from '@angular/core';
import {TimezoneService} from "../services/timezone.service";

@Injectable({
  providedIn: 'root'
})
export class UtilService {
  constructor(private timezoneService: TimezoneService) {}

}

